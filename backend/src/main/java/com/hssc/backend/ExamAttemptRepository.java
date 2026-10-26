package com.hssc.backend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt,UUID> {
    Optional<ExamAttempt> findByExamIdAndStudentId(UUID examId,UUID studentId);
    Optional<ExamAttempt> findByExamIdAndParticipantId(UUID examId,UUID participantId);
    Optional<ExamAttempt> findByExamIdAndSchoolId(UUID examId,UUID schoolId);
    List<ExamAttempt> findByParticipantIdOrderByStartedAtDesc(UUID participantId);
    List<ExamAttempt> findByParticipantSchoolIdOrderByStartedAtDesc(UUID schoolId);
    List<ExamAttempt> findByExamIdAndParticipantSchoolId(UUID examId,UUID schoolId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ExamAttempt a where a.exam.id = :examId and a.student.id = :studentId")
    Optional<ExamAttempt> findByExamIdAndStudentIdForUpdate(@Param("examId") UUID examId,@Param("studentId") UUID studentId);
    Optional<ExamAttempt> findByPublicCode(String code);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ExamAttempt a where a.exam.id = :examId and a.school.id = :schoolId")
    Optional<ExamAttempt> findByExamIdAndSchoolIdForUpdate(@Param("examId") UUID examId,@Param("schoolId") UUID schoolId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ExamAttempt a where a.publicCode = :code")
    Optional<ExamAttempt> findByPublicCodeForUpdate(@Param("code") String code);
    List<ExamAttempt> findByExamIdAndStatus(UUID examId,AttemptStatus status);
    List<ExamAttempt> findByStudentIdOrderByStartedAtDesc(UUID studentId);
    List<ExamAttempt> findByStatus(AttemptStatus status);
    List<ExamAttempt> findByExamIdOrderByStartedAtDesc(UUID examId);
    long countByStatusNot(AttemptStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ExamAttempt a where a.exam.id = :examId and a.status = :status")
    List<ExamAttempt> findByExamIdAndStatusForUpdate(@Param("examId") UUID examId,@Param("status") AttemptStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ExamAttempt a where a.status = :status")
    List<ExamAttempt> findByStatusForUpdate(@Param("status") AttemptStatus status);
    List<ExamAttempt> findByExamIdAndStudentSchoolZoneDistrictId(UUID examId,UUID districtId);
    List<ExamAttempt> findBySchoolIdOrderByStartedAtDesc(UUID schoolId);
}
