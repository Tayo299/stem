package com.hssc.backend;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ExamAssignmentRepository extends JpaRepository<ExamAssignment,UUID> { List<ExamAssignment> findByExamIdOrderByDisplayOrder(UUID examId); }
