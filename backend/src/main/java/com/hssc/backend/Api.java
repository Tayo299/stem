package com.hssc.backend;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;

public final class Api {
    private Api() {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record TokenResponse(String accessToken, String tokenType, Instant expiresAt, UUID userId, String role, String status) {}
    public record SchoolRegistration(@NotBlank @Email String email, @NotBlank @Size(min=12,max=128) String password,
            @NotBlank @Size(min=12,max=128) String confirmPassword,
            @NotBlank @Size(max=180) String name, @Size(max=80) String registrationNumber,
            @NotBlank @Size(max=255) String address, @NotNull UUID zoneId,
            @Size(max=180) String principalName, @NotBlank @Email @Size(max=254) String principalEmail,
            @NotBlank @Pattern(regexp="^(?=(?:\\D*\\d){7,15}\\D*$)[+()0-9 .-]+$") @Size(max=40) String principalPhone,
            @NotBlank @Size(max=180) String teacherInChargeName,
            @NotBlank @Pattern(regexp="^(?=(?:\\D*\\d){7,15}\\D*$)[+()0-9 .-]+$") @Size(max=40) String teacherInChargePhone) {}
    public record StudentRegistration(@NotBlank @Email String email, @NotBlank @Size(min=12,max=128) String password,
            @NotBlank @Size(min=12,max=128) String confirmPassword,
            @NotBlank @Size(max=30) String studentNumber, @Size(max=100) String firstName,
            @Size(max=100) String lastName, LocalDate dateOfBirth, @NotNull UUID schoolId,
            @Size(max=200) String fullName,@Min(1) @Max(120) Integer age,@Size(max=40) String gender,
            @NotBlank @Size(max=200) String parentGuardianName,
            @NotBlank @Pattern(regexp="^(?=(?:\\D*\\d){7,15}\\D*$)[+()0-9 .-]+$") @Size(max=40) String guardianPhone,
            @NotBlank @Size(max=500) String homeAddress,
            @Pattern(regexp="^(?=(?:\\D*\\d){7,15}\\D*$)[+()0-9 .-]+$") @Size(max=40) String phone) {}
    public record SchoolStudentProfileRequest(@NotBlank @Size(max=30) String studentNumber,
            @NotBlank @Size(max=200) String fullName,@NotNull @Min(1) @Max(120) Integer age,
            @NotBlank @Size(max=40) String gender) {}
    public record RegistrationResponse(UUID id, String status, boolean possibleDuplicate, String message) {}
    public record QuestionInput(@NotBlank @Size(max=3000) String prompt, @NotBlank @Size(max=80) String subject,
            @Min(1) @Max(1000) int points, @NotNull @Size(min=2,max=8) Map<@NotBlank String,@NotBlank String> options,
            String correctOption, @Min(1) @Max(400) Integer sourceNumber, Boolean anonymous) {
        public QuestionInput(String prompt,String subject,int points,Map<String,String> options,String correctOption){
            this(prompt,subject,points,options,correctOption,null,false);
        }
        public QuestionInput(String prompt,String subject,int points,Map<String,String> options,String correctOption,Integer sourceNumber){
            this(prompt,subject,points,options,correctOption,sourceNumber,false);
        }
    }
    public record BankRequest(@NotBlank @Size(max=120) String name) {}
    public record BankResponse(UUID id,String name,boolean active) {}
    public record QuestionResponse(UUID id,String prompt,String subject,int points,Map<String,String> options,String correctOption,
            Integer sourceNumber,boolean anonymous) {}
    public record QuestionSetResponse(UUID id,String name,boolean active,int questionCount,int keyedQuestionCount,int anonymousQuestionCount) {}
    public record PublicQuestion(UUID id,int order,String prompt,int points,Map<String,String> options) {}
    public record FrontendActiveQuestion(UUID id,int order,String questionText,Map<String,String> options,String selectedOption) {}
    public record FrontendActiveExam(UUID id,String status,List<FrontendActiveQuestion> questions,int remainingSeconds,String attemptCode,
            Instant startedAt,Instant deadline,UUID participantId) {}
    public record ParticipantExamStatus(UUID participantId,String participantName,String attemptStatus) {}
    public record StudentExamStatus(boolean portalOpen,UUID examId,String examTitle,int questionCount,int durationMinutes,
            BigDecimal marksPerCorrectAnswer,String attemptStatus,Instant opensAt,Instant closesAt,String scheduleStatus,
            List<ParticipantExamStatus> participants) {}
    public record ExamTimeResponse(int remainingSeconds) {}
    public record ExamEventRequest(@NotNull ExamEventReason reason,String attemptCode) {}
    public enum ExamEventReason { PAGE_LEFT, BROWSER_REFRESH }
    public record ExamRequest(@NotBlank @Size(max=160) String title,@NotNull UUID bankId,@NotNull UUID districtId,
            @NotNull Instant opensAt,@NotNull Instant closesAt,@Min(1) @Max(200) int questionCount,
            boolean anonymousAllowed) {}
    public record ExamResponse(UUID id,String title,Instant opensAt,Instant closesAt,int durationMinutes,
            int questionCount,String status,UUID districtId,String districtName,UUID bankId,String bankName) {}
    public record StartAttemptRequest(@NotNull UUID examId) {}
    public record FrontendStartRequest(UUID examId,UUID participantId) {
        public FrontendStartRequest(UUID examId){this(examId,null);}
    }
    public record FrontendAnswerRequest(@NotBlank @Size(max=1) String selectedOption,String attemptCode) {}
    public record FrontendSubmitRequest(String attemptCode) {}
    public record AttemptResponse(String attemptCode,Instant startedAt,Instant deadline,List<PublicQuestion> questions) {}
    public record AnswerRequest(@NotBlank String attemptCode,@NotNull UUID questionId,@NotBlank @Size(max=1) String selectedOption) {}
    public record SubmitAttemptRequest(@NotBlank String attemptCode) {}
    public record ResultResponse(String attemptCode,String status,String candidateName,UUID candidateId,String examTitle,
            BigDecimal score,BigDecimal possiblePoints,Instant submittedAt,boolean released,
            Integer totalQuestions,Integer attemptedQuestions,Integer answeredQuestions,Integer unansweredQuestions,Integer correctAnswers,Integer incorrectAnswers,
            BigDecimal marksPerCorrectAnswer,BigDecimal percentage,Instant startedAt,long timeSpentSeconds,String submissionType) {}
    public record AdminExamResult(UUID examId,String examTitle,UUID attemptId,String attemptCode,UUID studentId,String studentName,String studentNumber,
            String schoolName,String districtName,String status,BigDecimal score,BigDecimal possiblePoints,Instant startedAt,
            Instant submittedAt,long timeSpentSeconds,String submissionType,int totalQuestions,int attemptedQuestions,
            int answeredQuestions,int unansweredQuestions,int correctAnswers,int incorrectAnswers,int anonymousBlankCredits,
            BigDecimal marksPerCorrectAnswer,
            BigDecimal percentage,boolean released) {}
    public record NotificationResponse(UUID id,String type,String message,boolean read,Instant createdAt,String actionUrl) {}
    public record PortalRequest(boolean enabled) {}
    public record PolicyRequest(@Min(1) @Max(1000) int qualifiersPerDistrict,@NotNull List<String> tieBreakOrder) {}
    public record LeaderboardEntry(String studentName,String schoolName,String zoneName,String districtName,
            BigDecimal score,Integer rank,Boolean qualified) {}
    public record LeaderboardResponse(UUID examId,UUID districtId,List<LeaderboardEntry> entries,boolean qualificationDecisionPending,boolean published) {}
    public record LeaderboardPublishRequest(@NotNull UUID examId,@NotNull @Size(min=1,max=20) List<@NotNull UUID> schoolIds) {}
    public record LeaderboardSchoolCandidate(UUID id,String name,String zoneName,String districtName,BigDecimal topScore,int finalizedResults) {}
    public record AdminLeaderboardResponse(List<ExamResponse> exams,UUID examId,List<LeaderboardSchoolCandidate> candidates,
            List<UUID> selectedSchoolIds,boolean published) {}
    public record UploadResponse(UUID id,String originalName,String contentType,long size,String sha256) {}
    public record ZoneResponse(UUID id,String name) {}
    public record DistrictResponse(UUID id,String code,String name,List<ZoneResponse> zones) {}
    public record AccountReview(UUID id,String email,String role,String status,Instant createdAt) {}
    public record StudentSummary(UUID id,String studentNumber,String firstName,String lastName,String status,
            String fullName,Integer age,String gender) {}
    public record AuditRecord(UUID id,UUID actorId,String action,String entityType,String entityId,Instant createdAt) {}
    public record DocumentMetadata(UUID id,UUID ownerId,String originalName,String contentType,long size,String sha256,Instant createdAt) {}
    public record SponsorRequest(@NotBlank @Size(max=160) String name,@Size(max=180) String organization,
            @Size(max=1000) String description,@Size(max=500) String websiteUrl,@Min(0) int displayOrder,boolean active) {}
    public record SponsorResponse(UUID id,String name,String organization,String description,String websiteUrl,int displayOrder,boolean active) {}
    public record ContentPolicyRequest(@NotBlank @Size(max=100) String slug,@NotBlank @Size(max=180) String title,
            @NotBlank @Size(max=10000) String content,boolean active,@Min(0) int displayOrder) {}
    public record ContentPolicyResponse(UUID id,String slug,String title,String content,boolean active,int displayOrder) {}
    public record SchoolDirectoryEntry(UUID id,String name,String registrationNumber,String address,UUID zoneId,String zoneName,
            UUID districtId,String districtName,String districtCode) {}
    public record SchoolAdminDetail(UUID id,UUID accountId,String name,String registrationNumber,String address,String status,String loginEmail,
            String principalName,String principalEmail,String principalPhone,String teacherInChargeName,String teacherInChargePhone,UUID principalSignatureDocumentId,
            UUID zoneId,String zoneName,UUID districtId,String districtName,long studentCount) {}
    public record AdminStudentDetail(UUID id,String email,String fullName,String studentNumber,Integer age,String gender,String phone,
            String parentGuardianName,String guardianPhone,String homeAddress,String status,List<DocumentMetadata> documents) {}
    public record ParticipatingStudentRegistration(@NotBlank @Size(max=200) String fullName,
            @NotBlank @Size(max=40) String gender,@NotNull @Min(1) @Max(120) Integer age,
            @NotBlank @Size(max=200) String guardianName,
            @NotBlank @Pattern(regexp="^(?=(?:\\D*\\d){7,15}\\D*$)[+()0-9 .-]+$") @Size(max=40) String guardianPhone,
            @NotBlank @Size(max=500) String homeAddress) {
        public ParticipatingStudentRegistration(String fullName,String gender,Integer age) {
            this(fullName,gender,age,null,null,null);
        }
    }
    public record ParticipatingStudentResponse(UUID id,String fullName,String gender,int age,String guardianName,
            String guardianPhone,String homeAddress,UUID photoDocumentId) {}
    public record SchoolDashboard(UUID id,String name,String email,String status,String address,String registrationNumber,
            String principalName,String principalEmail,String principalPhone,String teacherInChargeName,String teacherInChargePhone,
            UUID principalSignatureDocumentId,String zoneName,String districtName,
            List<ParticipatingStudentResponse> participants) {}
    public record StudentDashboard(UUID studentId,String studentName,String schoolName,String accountStatus,
            String portalStatus,String examStatus,String resultStatus,String qualificationStatus,
            List<ExamResponse> activeExams,List<ResultResponse> results) {}
    public record AdminDashboard(long pendingAccounts,long activeSchools,long activeStudents,long exams,long attempts) {}
    public record AdminDistrictSummary(UUID id,String code,String name,int zoneCount,long schoolCount,long studentCount,
            boolean examAccessOpen) {}
    public record MessageResponse(String message) {}
}
