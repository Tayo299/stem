package com.hssc.backend;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface QuestionRepository extends JpaRepository<Question,UUID> {
    List<Question> findByBankId(UUID bankId);
    List<Question> findByBankIdOrderBySourceNumberAsc(UUID bankId);
    Optional<Question> findByBankIdAndSourceNumber(UUID bankId,Integer sourceNumber);
}
