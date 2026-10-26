package com.hssc.backend;

import java.util.*;

final class ExamQuestionSelector {
    private static final int REGULAR_BANK_SIZE = 400;
    private static final int REGULAR_PER_CANDIDATE = 180;
    private static final int ANONYMOUS_PER_CANDIDATE = 20;

    private ExamQuestionSelector() {}

    static List<Question> select(List<Question> bankQuestions, Random random) {
        List<Question> regular = bankQuestions.stream().filter(question->!question.isAnonymous()).toList();
        List<Question> anonymous = bankQuestions.stream().filter(Question::isAnonymous).toList();
        List<Question> keyedRegular = regular.stream().filter(question->question.getCorrectOption()!=null).toList();
        List<String> missing = new ArrayList<>();
        if(regular.size()!=REGULAR_BANK_SIZE)
            missing.add("Set bank must contain exactly "+REGULAR_BANK_SIZE+" regular questions, found "+regular.size());
        if(keyedRegular.size()<REGULAR_PER_CANDIDATE)
            missing.add("Set bank needs at least "+REGULAR_PER_CANDIDATE+" keyed regular questions, found "+keyedRegular.size());
        if(anonymous.size()!=ANONYMOUS_PER_CANDIDATE)
            missing.add("Set bank must contain exactly "+ANONYMOUS_PER_CANDIDATE+" marked anonymous questions, found "+anonymous.size());
        if(anonymous.stream().anyMatch(question->question.getCorrectOption()!=null))
            missing.add("Anonymous questions must not have answer keys");
        List<Question> selected=new ArrayList<>(200);
        if(!missing.isEmpty()) throw ApiException.conflict("Question bank cannot generate the required 200-question exam: "+String.join("; ",missing));
        List<Question> shuffledRegular=new ArrayList<>(keyedRegular);
        Collections.shuffle(shuffledRegular,random);
        selected.addAll(shuffledRegular.subList(0,REGULAR_PER_CANDIDATE));
        selected.addAll(anonymous);
        Collections.shuffle(selected,random);
        return List.copyOf(selected);
    }
}
