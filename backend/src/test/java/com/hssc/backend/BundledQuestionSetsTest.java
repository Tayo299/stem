package com.hssc.backend;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BundledQuestionSetsTest {
    private final ObjectMapper mapper=new ObjectMapper();

    @Test
    void setAContainsAllQuestionsAndProvidedAnswerKeys() throws IOException {
        List<Api.QuestionInput> questions=load("set-a.json");
        assertEquals(400,questions.size());
        assertTrue(questions.stream().allMatch(question->question.correctOption()!=null));
        assertValidSourceNumbersAndOptions(questions);
    }

    @Test
    void setBContainsAllQuestionsAndProvidedAnswerKeys() throws IOException {
        List<Api.QuestionInput> questions=load("set-b.json");
        assertEquals(400,questions.size());
        assertEquals(400,questions.stream().filter(question->question.correctOption()!=null).count());
        assertEquals("D",questions.getFirst().correctOption());
        assertNotNull(questions.get(200).correctOption());
        assertValidSourceNumbersAndOptions(questions);
    }

    @Test
    void bothOfficialSetsCanGenerateAnExamFromTheBundledAnonymousPool() throws IOException {
        List<Api.QuestionInput> anonymous=load("anonymous-questions.json");
        assertEquals(20,anonymous.size());
        assertTrue(anonymous.stream().allMatch(question->Boolean.TRUE.equals(question.anonymous())
                && question.correctOption()==null && question.sourceNumber()==null));
        assertEquals(20,anonymous.stream().map(Api.QuestionInput::prompt).distinct().count());
        anonymous.forEach(question->{
            assertEquals(4,question.options().size());
            assertDoesNotThrow(()->QuestionBankService.validateQuestion(question));
        });
        for(String set:List.of("set-a.json","set-b.json")){
            QuestionBank bank=new QuestionBank(set,null);
            List<Question> pool=new ArrayList<>();
            for(Api.QuestionInput question:load(set)){
                pool.add(new Question(bank,question.prompt(),question.subject(),question.points(),"{}",
                        question.correctOption(),question.sourceNumber(),false));
            }
            for(Api.QuestionInput question:anonymous){
                pool.add(new Question(bank,question.prompt(),question.subject(),question.points(),"{}",
                        null,null,true));
            }
            assertEquals(200,ExamQuestionSelector.select(pool,new Random(17)).size(),set);
        }
    }

    private List<Api.QuestionInput> load(String filename) throws IOException {
        try(var stream=getClass().getResourceAsStream("/question-sets/"+filename)){
            assertNotNull(stream,"Bundled question set is missing");
            return mapper.readValue(stream,new TypeReference<>(){});
        }
    }

    private void assertValidSourceNumbersAndOptions(List<Api.QuestionInput> questions) {
        for(int i=0;i<questions.size();i++){
            Api.QuestionInput question=questions.get(i);
            assertEquals(i+1,question.sourceNumber());
            assertEquals(4,question.options().size());
            assertDoesNotThrow(()->QuestionBankService.validateQuestion(question));
        }
    }
}
