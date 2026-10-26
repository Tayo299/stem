package com.hssc.backend;

import java.math.BigDecimal;
import java.util.List;

final class ExamScoring {
    private ExamScoring() {}

    static Score calculate(List<QuestionOutcome> questions,BigDecimal marksPerQuestion) {
        int correct=(int)questions.stream().filter(question->!question.anonymous() && question.correct()).count();
        int anonymousBlankCredits=(int)questions.stream()
                .filter(question->question.anonymous() && !question.answered()).count();
        int attempted=(int)questions.stream().filter(QuestionOutcome::answered).count();
        int total=questions.size();
        BigDecimal possible=BigDecimal.valueOf(total).multiply(marksPerQuestion);
        BigDecimal score=BigDecimal.valueOf(correct+anonymousBlankCredits).multiply(marksPerQuestion);
        return new Score(score,possible,total,attempted,correct,anonymousBlankCredits);
    }

    record QuestionOutcome(boolean anonymous,boolean answered,boolean correct) {}
    record Score(BigDecimal score,BigDecimal possible,int total,int attempted,int correct,int anonymousBlankCredits) {}
}
