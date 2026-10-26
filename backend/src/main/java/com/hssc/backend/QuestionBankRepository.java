package com.hssc.backend;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface QuestionBankRepository extends JpaRepository<QuestionBank,UUID> {
    List<QuestionBank> findByActiveTrue();
    Optional<QuestionBank> findByName(String name);
}
