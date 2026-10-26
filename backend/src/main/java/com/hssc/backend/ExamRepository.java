package com.hssc.backend;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ExamRepository extends JpaRepository<Exam,UUID> {
    List<Exam> findByStatusIn(Collection<ExamStatus> statuses);
    long countByStatusNot(ExamStatus status);
    Optional<Exam> findFirstByStatusOrderByClosesAtDesc(ExamStatus status);
}
