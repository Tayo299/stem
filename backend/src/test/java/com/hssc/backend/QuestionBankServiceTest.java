package com.hssc.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class QuestionBankServiceTest {
    @Test
    void loadsBothOfficialSetsWithTheAnonymousPoolAndDoesNotDuplicateIt() {
        QuestionBankRepository banks=mock(QuestionBankRepository.class);
        QuestionRepository questions=mock(QuestionRepository.class);
        Map<String,QuestionBank> banksByName=new HashMap<>();
        Map<UUID,List<Question>> questionsByBank=new HashMap<>();
        when(banks.findByName(anyString())).thenAnswer(invocation->{
            String name=invocation.getArgument(0);
            QuestionBank bank=banksByName.computeIfAbsent(name,key->{
                QuestionBank created=mock(QuestionBank.class);
                when(created.getId()).thenReturn(UUID.randomUUID());
                when(created.getName()).thenReturn(key);
                when(created.isActive()).thenReturn(true);
                return created;
            });
            return Optional.of(bank);
        });
        when(questions.findByBankIdOrderBySourceNumberAsc(any())).thenAnswer(invocation->
                questionsByBank.computeIfAbsent(invocation.getArgument(0),ignored->new ArrayList<>()));
        when(questions.findByBankId(any())).thenAnswer(invocation->
                questionsByBank.computeIfAbsent(invocation.getArgument(0),ignored->new ArrayList<>()));
        when(questions.save(any(Question.class))).thenAnswer(invocation->{
            Question question=invocation.getArgument(0);
            questionsByBank.computeIfAbsent(question.getBank().getId(),ignored->new ArrayList<>()).add(question);
            return question;
        });
        QuestionBankService service=new QuestionBankService(banks,questions,new ObjectMapper(),mock(AuditService.class));

        for(String set:List.of("A","B")){
            Api.QuestionSetResponse loaded=service.loadOfficialSet(set,null);
            assertEquals(420,loaded.questionCount());
            assertEquals(400,loaded.keyedQuestionCount());
            assertEquals(20,loaded.anonymousQuestionCount());

            Api.QuestionSetResponse reloaded=service.loadOfficialSet(set,null);
            assertEquals(420,reloaded.questionCount());
            assertEquals(400,reloaded.keyedQuestionCount());
            assertEquals(20,reloaded.anonymousQuestionCount());
        }
    }
}
