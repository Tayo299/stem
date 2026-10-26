package com.hssc.backend;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AnswerRepository extends JpaRepository<Answer,UUID> {
    Optional<Answer> findByAttemptIdAndQuestionId(UUID attemptId,UUID questionId);
    List<Answer> findByAttemptId(UUID attemptId);
}
