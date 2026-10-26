package com.hssc.backend;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ExamQuestionSelectorTest {
    @Test
    void selectsOneHundredEightyKeyedRegularAndTwentyUnkeyedAnonymousQuestions() {
        List<Question> pool=validPool();

        List<Question> selected=ExamQuestionSelector.select(pool,new Random(4));

        assertEquals(200,selected.size());
        assertEquals(200,selected.stream().map(Question::getPrompt).distinct().count());
        assertEquals(180,selected.stream().filter(q->!q.isAnonymous()).count());
        assertEquals(20,selected.stream().filter(Question::isAnonymous).count());
        assertTrue(selected.stream().filter(q->!q.isAnonymous()).allMatch(q->q.getCorrectOption()!=null));
        assertTrue(selected.stream().filter(Question::isAnonymous).allMatch(q->q.getCorrectOption()==null));
    }

    @Test
    void randomizesRegularQuestionsForSeparateCandidates() {
        List<Question> pool=validPool();
        List<Question> first=ExamQuestionSelector.select(pool,new Random(12));
        List<Question> second=ExamQuestionSelector.select(pool,new Random(27));

        assertNotEquals(
                first.stream().filter(q->!q.isAnonymous()).map(Question::getPrompt).toList(),
                second.stream().filter(q->!q.isAnonymous()).map(Question::getPrompt).toList());
    }

    @Test
    void allowsUnusedUnkeyedRegularQuestionsWhenAtLeastOneHundredEightyAreKeyed() {
        List<Question> pool=validPool();
        pool.stream().filter(question->!question.isAnonymous()).skip(220)
                .forEach(question->question.update(question.getPrompt(),question.getSubject(),
                        question.getPoints(),question.getOptionsJson(),null,false));

        List<Question> selected=ExamQuestionSelector.select(pool,new Random(9));

        assertEquals(200,selected.size());
        assertTrue(selected.stream().filter(question->!question.isAnonymous())
                .allMatch(question->question.getCorrectOption()!=null));
    }

    @Test
    void rejectsBanksMissingRequiredKeysOrAnonymousQuestions() {
        List<Question> incomplete=validPool().stream().filter(q->!q.isAnonymous())
                .filter(q->q.getSourceNumber()<=179).toList();

        ApiException error=assertThrows(ApiException.class,()->ExamQuestionSelector.select(incomplete,new Random(1)));
        assertTrue(error.getMessage().contains("at least 180 keyed regular questions"));
        assertTrue(error.getMessage().contains("marked anonymous questions"));
    }

    private List<Question> validPool() {
        QuestionBank bank=new QuestionBank("selection test",null);
        List<Question> pool=new ArrayList<>();
        int number=1;
        for(int i=0;i<400;i++)
            pool.add(new Question(bank,"General STEM regular "+i,"General STEM",1,"{}", "A",number++,false));
        for(int i=0;i<20;i++)
            pool.add(new Question(bank,"Anonymous question "+i,"Anonymous",1,"{}",null,null,true));
        return pool;
    }
}
