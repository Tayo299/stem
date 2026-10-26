package com.hssc.backend;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface StudentRepository extends JpaRepository<Student,UUID> {
    Optional<Student> findByAccountId(UUID accountId);
    boolean existsByStudentNumberIgnoreCaseAndSchoolId(String number,UUID schoolId);
    boolean existsByAccountEmailIgnoreCase(String email);
    List<Student> findBySchoolId(UUID schoolId);
    long countBySchoolId(UUID schoolId);
    long countByAccountStatus(AccountStatus status);
}
