package com.hssc.backend;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class QuestionValidationTest {
    @Test
    void acceptsValidQuestionOptionsRegardlessOfLabelCase() {
        Api.QuestionInput input=new Api.QuestionInput("Prompt","Science",2,Map.of("a","Option A","B","Option B"),"A");
        assertDoesNotThrow(()->QuestionBankService.validateQuestion(input));
    }

    @Test
    void acceptsQuestionWithoutAnswerKey() {
        Api.QuestionInput input=new Api.QuestionInput("Prompt","Science",1,Map.of("A","Option A","B","Option B"),null,1);
        assertDoesNotThrow(()->QuestionBankService.validateQuestion(input));
    }

    @Test
    void rejectsCorrectOptionMissingFromQuestion() {
        Api.QuestionInput input=new Api.QuestionInput("Prompt","Science",2,Map.of("A","Option A","B","Option B"),"C");
        assertThrows(ApiException.class,()->QuestionBankService.validateQuestion(input));
    }

    @Test
    void rejectsLabelsThatCollideAfterNormalization() {
        Api.QuestionInput input=new Api.QuestionInput("Prompt","Science",2,Map.of("a","Option A","A","Option B"),"A");
        assertThrows(ApiException.class,()->QuestionBankService.validateQuestion(input));
    }

    @Test
    void questionWithoutAnswerKeyIsNeverMarkedCorrect() {
        Question question=new Question(new QuestionBank("Test",null),"Prompt","Science",1,"{}",null,1);
        assertFalse(question.isCorrect("A"));
    }
}
