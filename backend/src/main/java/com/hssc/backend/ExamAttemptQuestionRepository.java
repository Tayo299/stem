package com.hssc.backend;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ExamAttemptQuestionRepository extends JpaRepository<ExamAttemptQuestion,UUID> {
    List<ExamAttemptQuestion> findByAttemptIdOrderByDisplayOrder(UUID attemptId);
}
