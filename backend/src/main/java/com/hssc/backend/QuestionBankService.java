package com.hssc.backend;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.util.*;

@Service
public class QuestionBankService {
    private final QuestionBankRepository banks; private final QuestionRepository questions; private final ObjectMapper mapper; private final AuditService audit;
    public QuestionBankService(QuestionBankRepository banks,QuestionRepository questions,ObjectMapper mapper,AuditService audit){
        this.banks=banks;this.questions=questions;this.mapper=mapper;this.audit=audit;
    }
    @Transactional
    public Api.BankResponse create(Api.BankRequest request,UserAccount actor){
        QuestionBank bank=banks.save(new QuestionBank(request.name().trim(),actor));audit.record(actor,"CREATE","QuestionBank",bank.getId().toString());
        return new Api.BankResponse(bank.getId(),bank.getName(),bank.isActive());
    }
    @Transactional
    public Api.BankResponse createPlaceholderBank(UserAccount actor){
        Api.BankResponse existing=banks.findAll().stream().filter(bank->bank.getName().equals("HSSC placeholder testing questions"))
                .findFirst().map(bank->new Api.BankResponse(bank.getId(),bank.getName(),bank.isActive())).orElse(null);
        if(existing!=null) return existing;
        Api.BankResponse bank=create(new Api.BankRequest("HSSC placeholder testing questions"),actor);
        List<Api.QuestionInput> placeholders=new ArrayList<>();
        for(int i=1;i<=200;i++){
            String number=String.format(Locale.ROOT,"%03d",i);
            placeholders.add(new Api.QuestionInput("Placeholder STEM question "+number+". Select the correct placeholder response.",
                    "General STEM",1,Map.of("A","Placeholder answer A","B","Placeholder answer B",
                            "C","Placeholder answer C","D","Placeholder answer D"),"A"));
        }
        importQuestions(bank.id(),placeholders,actor);
        return bank;
    }
    @Transactional
    public Api.QuestionSetResponse loadOfficialSet(String setCode,UserAccount actor){
        String code=setCode.toUpperCase(Locale.ROOT);
        if(!code.equals("A") && !code.equals("B")) throw ApiException.notFound("Question set not found");
        String name="HSSC STEM Competition Set "+code;
        List<Api.QuestionInput> input;
        try(var stream=new ClassPathResource("question-sets/set-"+code.toLowerCase(Locale.ROOT)+".json").getInputStream()){
            input=mapper.readValue(stream,new TypeReference<>(){});
        }catch(IOException e){throw new IllegalStateException("Question set "+code+" could not be loaded",e);}
        if(input.size()!=400) throw new IllegalStateException("Question set "+code+" must contain exactly 400 questions");
        List<Api.QuestionInput> anonymousInput;
        try(var stream=new ClassPathResource("question-sets/anonymous-questions.json").getInputStream()){
            anonymousInput=mapper.readValue(stream,new TypeReference<>(){});
        }catch(IOException e){throw new IllegalStateException("The anonymous question pool could not be loaded",e);}
        if(anonymousInput.size()!=20) throw new IllegalStateException("The anonymous question pool must contain exactly 20 questions");
        Set<String> anonymousPrompts=new HashSet<>();
        for(Api.QuestionInput question:anonymousInput){
            validateQuestion(question);
            if(!Boolean.TRUE.equals(question.anonymous()) || question.correctOption()!=null
                    || question.sourceNumber()!=null || !anonymousPrompts.add(question.prompt().trim()))
                throw new IllegalStateException("The anonymous question pool contains an invalid, keyed, or duplicate question");
        }
        NavigableMap<Integer,Api.QuestionInput> byNumber=new TreeMap<>();
        for(Api.QuestionInput question:input){
            validateQuestion(question);
            if(question.sourceNumber()==null || byNumber.putIfAbsent(question.sourceNumber(),question)!=null)
                throw new IllegalStateException("Question set "+code+" has an invalid or duplicate source number");
        }
        if(byNumber.size()!=400 || byNumber.firstKey()!=1 || byNumber.lastKey()!=400)
            throw new IllegalStateException("Question set "+code+" must contain question numbers 1 through 400");
        QuestionBank bank=banks.findByName(name).orElseGet(()->banks.save(new QuestionBank(name,actor)));
        Map<Integer,Question> existing=new HashMap<>();
        List<Question> existingQuestions=questions.findByBankIdOrderBySourceNumberAsc(bank.getId());
        if(existingQuestions.stream().anyMatch(question->question.getSourceNumber()==null && !question.isAnonymous()))
            throw ApiException.conflict("The official question bank contains regular questions without source numbers");
        existingQuestions.stream()
                .filter(question->question.getSourceNumber()!=null)
                .forEach(question->existing.put(question.getSourceNumber(),question));
        Map<String,Question> existingAnonymous=new HashMap<>();
        existingQuestions.stream().filter(Question::isAnonymous).filter(question->question.getSourceNumber()==null)
                .forEach(question->existingAnonymous.put(question.getPrompt(),question));
        for(var entry:byNumber.entrySet()){
            Api.QuestionInput inputQuestion=entry.getValue();
            Question question=existing.get(entry.getKey());
            String options=writeOptions(inputQuestion.options());
            if(question==null){
            questions.save(new Question(bank,inputQuestion.prompt().trim(),inputQuestion.subject().trim(),
                    inputQuestion.points(),options,normalizeCorrectOption(inputQuestion.correctOption()),entry.getKey(),
                    Boolean.TRUE.equals(inputQuestion.anonymous())));
            }else{
            boolean anonymous=inputQuestion.anonymous()==null?question.isAnonymous():inputQuestion.anonymous();
            String correctOption=inputQuestion.correctOption()==null?question.getCorrectOption():inputQuestion.correctOption();
            if(anonymous) correctOption=null;
            question.update(inputQuestion.prompt().trim(),inputQuestion.subject().trim(),inputQuestion.points(),
                    options,normalizeCorrectOption(correctOption),anonymous);
            }
        }
        for(Api.QuestionInput inputQuestion:anonymousInput){
            String prompt=inputQuestion.prompt().trim();
            String options=writeOptions(inputQuestion.options());
            Question question=existingAnonymous.get(prompt);
            if(question==null){
                questions.save(new Question(bank,prompt,inputQuestion.subject().trim(),inputQuestion.points(),
                        options,null,null,true));
            }else{
                question.update(prompt,inputQuestion.subject().trim(),inputQuestion.points(),options,null,true);
            }
        }
        audit.record(actor,"LOAD_OFFICIAL_SET","QuestionBank",bank.getId().toString());
        List<Question> loaded=questions.findByBankId(bank.getId());
        return new Api.QuestionSetResponse(bank.getId(),bank.getName(),bank.isActive(),loaded.size(),
                (int)loaded.stream().filter(question->question.getCorrectOption()!=null).count(),
                (int)loaded.stream().filter(Question::isAnonymous).count());
    }
    @Transactional(readOnly=true)
    public List<Api.BankResponse> list(){return banks.findAll().stream().map(b->new Api.BankResponse(b.getId(),b.getName(),b.isActive())).toList();}
    @Transactional
    public List<Api.QuestionResponse> importQuestions(UUID bankId,List<Api.QuestionInput> input,UserAccount actor){
        QuestionBank bank=banks.findById(bankId).orElseThrow(()->ApiException.notFound("Question bank not found"));
        if(!bank.isActive()) throw ApiException.conflict("Question bank is inactive");
        if(input==null || input.isEmpty() || input.size()>1000) throw ApiException.badRequest("Import must contain between 1 and 1000 questions");
        Set<Integer> sourceNumbers=new HashSet<>();
        for(Api.QuestionInput q:input){
            validateQuestion(q);
            if(q.sourceNumber()!=null && !sourceNumbers.add(q.sourceNumber()))
                throw ApiException.badRequest("Question source numbers must be unique within an import");
        }
        List<Question> saved=input.stream().map(q->{
            Question existing=q.sourceNumber()==null?null:questions.findByBankIdAndSourceNumber(bankId,q.sourceNumber()).orElse(null);
            if(existing!=null){
                boolean anonymous=q.anonymous()==null?existing.isAnonymous():q.anonymous();
                String correctOption=q.correctOption()==null?existing.getCorrectOption():q.correctOption();
                if(anonymous) correctOption=null;
                existing.update(q.prompt().trim(),q.subject().trim(),q.points(),writeOptions(q.options()),
                        normalizeCorrectOption(correctOption),anonymous);
                return existing;
            }
            return questions.save(new Question(bank,q.prompt().trim(),q.subject().trim(),q.points(),writeOptions(q.options()),
                    normalizeCorrectOption(q.correctOption()),q.sourceNumber(),Boolean.TRUE.equals(q.anonymous())));
        }).toList();
        audit.record(actor,"IMPORT_QUESTIONS","QuestionBank",bankId.toString());
        return saved.stream().map(this::adminQuestion).toList();
    }
    @Transactional(readOnly=true)
    public List<Api.QuestionResponse> questions(UUID bankId){
        return questions.findByBankId(bankId).stream().map(this::adminQuestion).toList();
    }
    @Transactional
    public Api.BankResponse setActive(UUID id,boolean active,UserAccount actor){
        QuestionBank bank=banks.findById(id).orElseThrow(()->ApiException.notFound("Question bank not found"));
        bank.setActive(active);audit.record(actor,active?"ACTIVATE":"DEACTIVATE","QuestionBank",id.toString());
        return new Api.BankResponse(id,bank.getName(),bank.isActive());
    }
    public Map<String,String> readOptions(Question question){
        try{return mapper.readValue(question.getOptionsJson(),new TypeReference<>(){});}
        catch(Exception e){throw new IllegalStateException("Stored question options are invalid",e);}
    }
    private Api.QuestionResponse adminQuestion(Question q){return new Api.QuestionResponse(q.getId(),q.getPrompt(),q.getSubject(),q.getPoints(),readOptions(q),q.getCorrectOption(),q.getSourceNumber(),q.isAnonymous());}
    private static String normalizeCorrectOption(String correctOption){
        return correctOption==null?null:correctOption.toUpperCase(Locale.ROOT);
    }
    private String writeOptions(Map<String,String> options){
        try{
            Map<String,String> normalized=new TreeMap<>();
            options.forEach((key,value)->normalized.put(key.toUpperCase(Locale.ROOT),value));
            return mapper.writeValueAsString(normalized);
        }catch(Exception e){throw new IllegalStateException(e);}
    }
    public static void validateQuestion(Api.QuestionInput q){
        if(q==null || q.prompt()==null || q.prompt().isBlank() || q.prompt().length()>3000
                || q.subject()==null || q.subject().isBlank() || q.subject().length()>80)
            throw ApiException.badRequest("Question prompt and subject are required");
        if(q.sourceNumber()!=null && (q.sourceNumber()<1 || q.sourceNumber()>400))
            throw ApiException.badRequest("Question source number must be between 1 and 400");
        if(q.points()<1 || q.points()>1000) throw ApiException.badRequest("Question points must be between 1 and 1000");
        if(q.options()==null || q.options().size()<2 || q.options().size()>8)
            throw ApiException.badRequest("Questions require 2-8 answer options");
        Set<String> normalized=new HashSet<>();
        for(var entry:q.options().entrySet()){
            String key=entry.getKey().toUpperCase(Locale.ROOT);
            if(!key.matches("[A-H]") || entry.getValue()==null || entry.getValue().isBlank() || !normalized.add(key))
                throw ApiException.badRequest("Answer options must have unique A-H labels and non-empty text");
        }
        if(q.correctOption()!=null && (q.correctOption().isBlank()
                || !normalized.contains(q.correctOption().toUpperCase(Locale.ROOT))))
            throw ApiException.badRequest("Correct option must match an available option label");
        if(Boolean.TRUE.equals(q.anonymous()) && q.correctOption()!=null)
            throw ApiException.badRequest("Anonymous questions cannot have an answer key");
    }
}
