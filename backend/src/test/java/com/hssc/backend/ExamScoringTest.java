package com.hssc.backend;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExamScoringTest {
    @Test
    void awardsBlankAnonymousItemsButNotAttemptedOnes() {
        List<ExamScoring.QuestionOutcome> questions=List.of(
                new ExamScoring.QuestionOutcome(false,true,true),
                new ExamScoring.QuestionOutcome(false,true,false),
                new ExamScoring.QuestionOutcome(false,false,false),
                new ExamScoring.QuestionOutcome(true,false,false),
                new ExamScoring.QuestionOutcome(true,true,false));

        ExamScoring.Score score=ExamScoring.calculate(questions,new BigDecimal("2.50"));

        assertEquals(new BigDecimal("5.00"),score.score());
        assertEquals(new BigDecimal("12.50"),score.possible());
        assertEquals(5,score.total());
        assertEquals(3,score.attempted());
        assertEquals(1,score.correct());
        assertEquals(1,score.anonymousBlankCredits());
    }
}
