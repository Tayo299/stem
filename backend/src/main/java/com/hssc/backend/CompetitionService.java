package com.hssc.backend;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import java.security.SecureRandom;
import java.time.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class CompetitionService {
    private final int examDurationMinutes;
    private final BigDecimal marksPerCorrectAnswer;
    private final ExamRepository exams; private final QuestionBankRepository banks; private final QuestionRepository questions;
    private final ExamAssignmentRepository assignments; private final ExamAttemptRepository attempts; private final AnswerRepository answers;
    private final StudentRepository students; private final QuestionBankService bankService; private final PortalService portal;
    private final SchoolRepository schools; private final SchoolParticipantRepository schoolParticipants;
    private final ExamAttemptQuestionRepository attemptQuestions;
    private final NotificationService notifications; private final AuditService audit; private final LeaderboardPolicyRepository policies;
    private final LeaderboardPublicationRepository leaderboardPublications;
    private final ExamEventRepository examEvents;
    private final ExamDistrictReleaseRepository districtReleases; private final DistrictRepository districts;
    private final SecureRandom random=new SecureRandom();
    public CompetitionService(ExamRepository exams,QuestionBankRepository banks,QuestionRepository questions,ExamAssignmentRepository assignments,
            ExamAttemptRepository attempts,AnswerRepository answers,StudentRepository students,SchoolRepository schools,
            SchoolParticipantRepository schoolParticipants,ExamAttemptQuestionRepository attemptQuestions,QuestionBankService bankService,
            PortalService portal,NotificationService notifications,AuditService audit,LeaderboardPolicyRepository policies,ExamEventRepository examEvents,
            ExamDistrictReleaseRepository districtReleases,DistrictRepository districts,
            LeaderboardPublicationRepository leaderboardPublications,
            @Value("${hssc.exam.duration-minutes:40}") int examDurationMinutes,
            @Value("${hssc.exam.marks-per-correct-answer:2.5}") BigDecimal marksPerCorrectAnswer){
        if(examDurationMinutes<1 || marksPerCorrectAnswer.signum()<=0) throw new IllegalStateException("Exam timing and marking configuration must be positive");
        try {
            this.marksPerCorrectAnswer=marksPerCorrectAnswer.setScale(2,RoundingMode.UNNECESSARY);
        } catch(ArithmeticException ex) {
            throw new IllegalStateException("Exam marks per correct answer must have at most two decimal places",ex);
        }
        if(this.marksPerCorrectAnswer.precision()>8)
            throw new IllegalStateException("Exam marks per correct answer exceeds the supported numeric range");
        this.examDurationMinutes=examDurationMinutes;
        this.leaderboardPublications=leaderboardPublications;
        this.exams=exams;this.banks=banks;this.questions=questions;this.assignments=assignments;this.attempts=attempts;this.answers=answers;
        this.students=students;this.schools=schools;this.schoolParticipants=schoolParticipants;this.attemptQuestions=attemptQuestions;
        this.bankService=bankService;this.portal=portal;this.notifications=notifications;this.audit=audit;this.policies=policies;
        this.examEvents=examEvents;this.districtReleases=districtReleases;this.districts=districts;
    }
    @Transactional
    public Api.ExamResponse createExam(Api.ExamRequest request,UserAccount actor){
        if(!request.closesAt().isAfter(request.opensAt())) throw ApiException.badRequest("Exam close time must be after open time");
        if(request.opensAt().isBefore(Instant.now())) throw ApiException.badRequest("Exam open time must be in the future");
        if(request.questionCount()!=200)
            throw ApiException.badRequest("Exams must contain 200 questions: 180 selected from the chosen set plus 20 anonymous questions");
        QuestionBank bank=banks.findById(request.bankId()).orElseThrow(()->ApiException.notFound("Question bank not found"));
        if(!bank.isActive()) throw ApiException.conflict("Question bank is inactive");
        if(isPlaceholderBank(bank)) throw ApiException.conflict("Placeholder question banks cannot be used for examination schedules");
        List<Question> bankQuestions=questions.findByBankId(bank.getId());
        ExamQuestionSelector.select(bankQuestions,random);
        District district=districts.findById(request.districtId()).orElseThrow(()->ApiException.notFound("District not found"));
        Exam exam=exams.save(new Exam(request.title().trim(),bank,district,request.opensAt(),request.closesAt(),examDurationMinutes,
                request.questionCount(),marksPerCorrectAnswer,request.anonymousAllowed()));
        List<Question> pool=new ArrayList<>(bankQuestions);
        pool.sort(Comparator.comparing(Question::getSourceNumber,Comparator.nullsLast(Comparator.naturalOrder())));
        for(int i=0;i<pool.size();i++) assignments.save(new ExamAssignment(exam,pool.get(i),i+1));
        audit.record(actor,"CREATE","Exam",exam.getId().toString());
        return response(exam);
    }
    @Transactional
    public Api.ExamResponse publish(UUID id,UserAccount actor){
        Exam exam=exams.findById(id).orElseThrow(()->ApiException.notFound("Exam not found"));
        if(exam.getStatus()!=ExamStatus.DRAFT) throw ApiException.conflict("Only draft exams can be published");
        if(isPlaceholderBank(exam.getBank())) throw ApiException.conflict("Placeholder question banks cannot be used for examination schedules");
        ExamQuestionSelector.select(assignments.findByExamIdOrderByDisplayOrder(id).stream().map(ExamAssignment::getQuestion).toList(),random);
        exam.setStatus(ExamStatus.OPEN);audit.record(actor,"PUBLISH","Exam",id.toString());
        return response(exam);
    }
    @Transactional(readOnly=true)
    public List<Api.ExamResponse> listExams(boolean administrator){
        Instant now=Instant.now();
        return exams.findAll().stream().filter(e->administrator || (!isPlaceholderBank(e.getBank())
                        && e.getStatus()==ExamStatus.OPEN && now.isBefore(e.getClosesAt())))
                .map(this::response).toList();
    }
    @Transactional(readOnly=true)
    public List<Api.ExamResponse> listExamsFor(UserAccount user){
        if(user.getRole()==Role.ADMIN) return listExams(true);
        UUID districtId=schoolFor(user).getZone().getDistrict().getId();
        return listExams(false).stream().filter(exam->Objects.equals(exam.districtId(),districtId)).toList();
    }
    @Transactional(readOnly=true)
    public List<Api.ExamResponse> activeExams(){
        Instant now=Instant.now();
        return exams.findByStatusIn(List.of(ExamStatus.OPEN)).stream()
                .filter(e->!isPlaceholderBank(e.getBank()) && !now.isBefore(e.getOpensAt()) && now.isBefore(e.getClosesAt()))
                .map(this::response).toList();
    }
    @Transactional(readOnly=true)
    public List<Api.ExamResponse> activeExamsFor(UserAccount user){
        if(user.getRole()==Role.ADMIN) return activeExams();
        UUID districtId=schoolFor(user).getZone().getDistrict().getId();
        if(!portal.districtEnabled("exam_access",districtId)) return List.of();
        return activeExams().stream().filter(exam->Objects.equals(exam.districtId(),districtId)).toList();
    }
    @Transactional(readOnly=true)
    public Api.ExamResponse chooseActiveExam(UUID requestedId){
        Instant now=Instant.now();
        if(requestedId!=null){
            Exam exam=exams.findById(requestedId).orElseThrow(()->ApiException.notFound("Exam not found"));
            if(isPlaceholderBank(exam.getBank())) throw ApiException.conflict("Placeholder question banks cannot be used for examination attempts");
            if(exam.getStatus()!=ExamStatus.OPEN || now.isBefore(exam.getOpensAt()) || !now.isBefore(exam.getClosesAt()))
                throw ApiException.conflict("Exam is not currently open");
            return response(exam);
        }
        List<Exam> open=exams.findByStatusIn(List.of(ExamStatus.OPEN)).stream()
                .filter(e->!isPlaceholderBank(e.getBank()) && !now.isBefore(e.getOpensAt()) && now.isBefore(e.getClosesAt()))
                .toList();
        if(open.isEmpty()) throw ApiException.notFound("No exam is currently open");
        if(open.size()>1) throw ApiException.conflict("More than one exam is open; provide examId");
        return response(open.getFirst());
    }
    @Transactional(readOnly=true)
    public List<Api.ResultResponse> studentResults(Student student){
        return attempts.findByStudentIdOrderByStartedAtDesc(student.getId()).stream()
                .map(this::result).toList();
    }
    @Transactional(readOnly=true)
    public List<Api.ResultResponse> resultsForAccount(UserAccount user){
        if(user.getRole()==Role.SCHOOL){
            School school=schoolFor(user);
            return java.util.stream.Stream.concat(
                    attempts.findByParticipantSchoolIdOrderByStartedAtDesc(school.getId()).stream(),
                    attempts.findBySchoolIdOrderByStartedAtDesc(school.getId()).stream())
                    .distinct().sorted(Comparator.comparing(ExamAttempt::getStartedAt).reversed())
                    .map(this::result).toList();
        }
        return studentResults(requireStudentIdentity(user));
    }
    @Transactional(readOnly=true)
    public Api.StudentExamStatus studentExamStatus(UserAccount user){
        School school=schoolFor(user);
        UUID districtId=school.getZone().getDistrict().getId();
        Exam exam;
        if(user.getRole()==Role.SCHOOL) {
            List<Exam> withCandidateAttempts=exams.findAll().stream().filter(e->e.getDistrict()!=null
                    && !isPlaceholderBank(e.getBank())
                    && e.getDistrict().getId().equals(districtId)
                    && (attempts.findByExamIdAndSchoolId(e.getId(),school.getId())
                            .filter(a->a.getStatus()==AttemptStatus.IN_PROGRESS).isPresent()
                        || attempts.findByExamIdAndParticipantSchoolId(e.getId(),school.getId()).stream()
                            .anyMatch(a->a.getStatus()==AttemptStatus.IN_PROGRESS))).toList();
            exam=withCandidateAttempts.stream().max(Comparator.comparing(Exam::getOpensAt)).orElseGet(()->
                    exams.findAll().stream().filter(e->e.getDistrict()!=null && !isPlaceholderBank(e.getBank())
                            && e.getDistrict().getId().equals(districtId)
                            && (e.getStatus()==ExamStatus.OPEN||e.getStatus()==ExamStatus.RESULTS_RELEASED))
                            .max(Comparator.comparing(Exam::getOpensAt)).orElse(null));
        } else {
            Student student=requireStudentIdentity(user);
            ExamAttempt activeAttempt=attempts.findByStudentIdOrderByStartedAtDesc(student.getId()).stream()
                    .filter(a->a.getStatus()==AttemptStatus.IN_PROGRESS && !isPlaceholderBank(a.getExam().getBank()))
                    .findFirst().orElse(null);
            exam=activeAttempt==null?exams.findAll().stream().filter(e->e.getDistrict()!=null
                    && !isPlaceholderBank(e.getBank())
                    && e.getDistrict().getId().equals(districtId)
                    && (e.getStatus()==ExamStatus.OPEN||e.getStatus()==ExamStatus.RESULTS_RELEASED))
                    .max(Comparator.comparing(Exam::getOpensAt)).orElse(null):activeAttempt.getExam();
        }
        if(exam==null) return new Api.StudentExamStatus(false,null,null,0,examDurationMinutes,marksPerCorrectAnswer,
                "NOT_STARTED",null,null,"NOT_SCHEDULED",List.of());
        Instant now=Instant.now();
        boolean districtEnabled=portal.districtEnabled("exam_access",districtId);
        List<Api.ParticipantExamStatus> candidateStatuses;
        String attemptStatus="NOT_STARTED";
        if(user.getRole()==Role.SCHOOL) {
            ExamAttempt teamAttempt=attempts.findByExamIdAndSchoolId(exam.getId(),school.getId()).orElse(null);
            candidateStatuses=schoolParticipants.findBySchoolIdOrderByFullNameAsc(school.getId()).stream().map(participant->{
                ExamAttempt attempt=teamAttempt!=null?teamAttempt:
                        attempts.findByExamIdAndParticipantId(exam.getId(),participant.getId()).orElse(null);
                return new Api.ParticipantExamStatus(participant.getId(),participant.getFullName(),
                        attempt==null?"NOT_STARTED":attempt.getStatus().name());
            }).toList();
            if(candidateStatuses.stream().anyMatch(p->p.attemptStatus().equals("IN_PROGRESS"))) attemptStatus="IN_PROGRESS";
            else if(candidateStatuses.stream().anyMatch(p->!p.attemptStatus().equals("NOT_STARTED")))
                attemptStatus=candidateStatuses.stream().filter(p->!p.attemptStatus().equals("NOT_STARTED")).findFirst().orElseThrow().attemptStatus();
        } else {
            Student student=requireStudentIdentity(user);
            ExamAttempt attempt=attempts.findByExamIdAndStudentId(exam.getId(),student.getId()).orElse(null);
            candidateStatuses=List.of(new Api.ParticipantExamStatus(student.getId(),student.getFullName(),
                    attempt==null?"NOT_STARTED":attempt.getStatus().name()));
            if(attempt!=null) attemptStatus=attempt.getStatus().name();
        }
        boolean isOpen=exam.getStatus()==ExamStatus.OPEN && districtEnabled
                && !now.isBefore(exam.getOpensAt()) && now.isBefore(exam.getClosesAt());
        boolean hasActiveAttempt=candidateStatuses.stream().anyMatch(p->p.attemptStatus().equals("IN_PROGRESS"));
        String scheduleStatus=now.isBefore(exam.getOpensAt())?"SCHEDULED":
                (!now.isBefore(exam.getClosesAt())||(exam.getStatus()==ExamStatus.RESULTS_RELEASED&&!hasActiveAttempt))?"CLOSED":
                !districtEnabled?"DISABLED":"OPEN";
        return new Api.StudentExamStatus(isOpen||hasActiveAttempt,exam.getId(),exam.getTitle(),exam.getQuestionCount(),
                exam.getDurationMinutes(),exam.getMarksPerCorrectAnswer(),attemptStatus,exam.getOpensAt(),exam.getClosesAt(),
                scheduleStatus,candidateStatuses);
    }
    @Transactional(readOnly=true)
    public Api.LeaderboardResponse latestLeaderboard(UUID districtId){
        return leaderboardPublications.findAll().stream().filter(LeaderboardPublication::isPublished)
                .map(publication->exams.findById(publication.getExamId()).orElse(null))
                .filter(Objects::nonNull).max(Comparator.comparing(Exam::getClosesAt))
                .map(exam->publishedLeaderboard(exam.getId(),districtId))
                .orElseGet(()->new Api.LeaderboardResponse(null,districtId,List.of(),false,false));
    }
    @Transactional
    public Api.FrontendActiveExam startForFrontend(UUID examId,UserAccount studentUser){
        return startForFrontend(examId,studentUser,null);
    }
    @Transactional
    public Api.FrontendActiveExam startForFrontend(UUID examId,UserAccount studentUser,UUID participantId){
        Api.ExamResponse exam=chooseActiveExam(examId);
        return startFrontendExam(exam.id(),studentUser,participantId);
    }
    @Transactional
    public Api.FrontendActiveExam startFrontendExam(UUID examId,UserAccount user,UUID participantId){
        if(user==null) throw ApiException.forbidden("An active school or student account is required");
        start(examId,user,participantId);
        ExamAttempt created=user.getRole()==Role.SCHOOL
                ?attempts.findByExamIdAndSchoolId(examId,schoolFor(user).getId())
                    .orElseThrow(()->ApiException.conflict("Exam attempt could not be created"))
                :attempts.findByExamIdAndStudentId(examId,requireStudentIdentity(user).getId())
                    .orElseThrow(()->ApiException.conflict("Exam attempt could not be created"));
        return frontendAttempt(created,user);
    }
    @Transactional
    public Api.FrontendActiveExam frontendAttempt(UUID examId,UserAccount user,UUID participantId){
        ExamAttempt attempt;
        if(user.getRole()==Role.SCHOOL){
            School school=schoolFor(user);
            attempt=attempts.findByExamIdAndSchoolId(examId,school.getId()).orElseGet(()->participantId==null?null:
                    attempts.findByExamIdAndParticipantId(examId,participantId).orElse(null));
            if(attempt==null) throw ApiException.notFound("No examination attempt exists yet");
        }else{
            Student student=requireStudentIdentity(user);
            attempt=attempts.findByExamIdAndStudentId(examId,student.getId())
                    .orElseThrow(()->ApiException.notFound("No examination attempt exists yet"));
        }
        return frontendAttempt(attempt,user);
    }
    private Api.FrontendActiveExam frontendAttempt(ExamAttempt attempt,UserAccount user){
        getAccessible(attempt,user);
        if(attempt.getStatus()==AttemptStatus.IN_PROGRESS && attempt.remainingSeconds(Instant.now())==0)
            finalizeExpired(attempt,Instant.now());
        Map<UUID,String> selected=new HashMap<>();
        answers.findByAttemptId(attempt.getId()).forEach(answer->selected.put(answer.getQuestion().getId(),answer.getSelectedOption()));
        List<Question> assigned=questionsForAttempt(attempt);
        List<Api.FrontendActiveQuestion> publicQuestions=new ArrayList<>();
        for(int i=0;i<assigned.size();i++){
            Question question=assigned.get(i);
            publicQuestions.add(new Api.FrontendActiveQuestion(question.getId(),i+1,question.getPrompt(),
                    bankService.readOptions(question),selected.get(question.getId())));
        }
        int remaining=attempt.remainingSeconds(Instant.now());
        return new Api.FrontendActiveExam(attempt.getExam().getId(),attempt.getStatus().name(),publicQuestions,remaining,
                attempt.getPublicCode(),attempt.getStartedAt(),deadline(attempt),candidateId(attempt));
    }
    @Transactional
    public Api.ExamTimeResponse time(UUID examId,String code,UserAccount user){
        ExamAttempt attempt=resolveAttemptForUpdate(examId,code,user);
        Instant now=Instant.now();
        int remaining=attempt.remainingSeconds(now);
        if(attempt.getStatus()==AttemptStatus.IN_PROGRESS && remaining==0) finish(attempt,now,AttemptStatus.TIMED_OUT);
        return new Api.ExamTimeResponse(remaining);
    }
    @Transactional
    public Api.MessageResponse recordExitEvent(UUID examId,Api.ExamEventRequest request,UserAccount user){
        ExamAttempt attempt=resolveAttemptForUpdate(examId,request.attemptCode(),user);
        examEvents.save(new ExamEvent(attempt,request.reason()));
        audit.record(user,"EXAM_EXIT_"+request.reason(),"ExamAttempt",attempt.getId().toString());
        return new Api.MessageResponse("Exit event recorded; the attempt remains available until its deadline");
    }
    @Transactional
    public Api.MessageResponse saveAnswerForExam(UUID examId,UUID questionId,String selectedOption,String code,UserAccount user){
        ExamAttempt attempt=resolveAttempt(examId,code,user);
        return saveAnswer(new Api.AnswerRequest(attempt.getPublicCode(),questionId,selectedOption),user);
    }
    @Transactional
    public Api.ResultResponse submitForExam(UUID examId,String code,UserAccount user){
        ExamAttempt attempt=resolveAttempt(examId,code,user);
        return submit(attempt.getPublicCode(),user);
    }
    @Transactional
    public Api.AttemptResponse start(UUID examId,UserAccount studentUser){
        return start(examId,studentUser,null);
    }
    @Transactional
    public Api.AttemptResponse start(UUID examId,UserAccount studentUser,UUID participantId){
        Exam exam=exams.findById(examId).orElseThrow(()->ApiException.notFound("Exam not found"));
        Instant now=Instant.now();
        if(exam.getStatus()!=ExamStatus.OPEN || now.isBefore(exam.getOpensAt()) || !now.isBefore(exam.getClosesAt()))
            throw ApiException.conflict("Exam is not currently open");
        if(exam.getDistrict()==null || !portal.districtEnabled("exam_access",exam.getDistrict().getId()))
            throw ApiException.conflict("Exam access is disabled for this district");
        Student student=null;
        SchoolParticipant participant=null;
        School school=null;
        List<SchoolParticipant> teamParticipants=List.of();
        if(studentUser==null){if(!exam.isAnonymousAllowed()) throw ApiException.forbidden("This exam does not allow anonymous attempts");}
        else{
            if(studentUser.getStatus()!=AccountStatus.ACTIVE) throw ApiException.forbidden("Account is not active");
            if(studentUser.getRole()==Role.SCHOOL){
                school=schoolFor(studentUser);
                if(!school.getZone().getDistrict().getId().equals(exam.getDistrict().getId()))
                    throw ApiException.forbidden("This examination is not scheduled for your school district");
                teamParticipants=schoolParticipants.findBySchoolIdOrderByFullNameAsc(school.getId());
                if(teamParticipants.size()!=2)
                    throw ApiException.conflict("Exactly two participating students must be registered before the school can start its shared examination");
                ExamAttempt existing=attempts.findByExamIdAndSchoolId(examId,school.getId()).orElse(null);
                if(existing!=null){
                    if(existing.getStatus()!=AttemptStatus.IN_PROGRESS)
                        throw ApiException.conflict("Your school has already completed this examination");
                    return new Api.AttemptResponse(existing.getPublicCode(),existing.getStartedAt(),
                            deadline(existing),publicQuestions(existing));
                }
            }else{
                student=requireStudentIdentity(studentUser);
                if(!student.getSchool().getZone().getDistrict().getId().equals(exam.getDistrict().getId()))
                    throw ApiException.forbidden("This examination is not scheduled for your school district");
                if(attempts.findByExamIdAndStudentId(examId,student.getId()).isPresent())
                    throw ApiException.conflict("A student may only attempt this exam once");
            }
        }
        ExamAttempt attempt=studentUser!=null && studentUser.getRole()==Role.SCHOOL
                ?new ExamAttempt(exam,null,null,school,UUID.randomUUID().toString(),now)
                :new ExamAttempt(exam,student,participant,UUID.randomUUID().toString(),now);
        if(!teamParticipants.isEmpty()) attempt.getTeamParticipants().addAll(teamParticipants);
        attempt=attempts.save(attempt);
        List<Question> selected=ExamQuestionSelector.select(assignments.findByExamIdOrderByDisplayOrder(examId).stream()
                .map(ExamAssignment::getQuestion).toList(),random);
        for(int i=0;i<selected.size();i++)
            attemptQuestions.save(new ExamAttemptQuestion(attempt,selected.get(i),i+1));
        audit.record(studentUser,"START_ATTEMPT","ExamAttempt",attempt.getId().toString());
        List<Api.PublicQuestion> publicQuestions=publicQuestions(attempt);
        return new Api.AttemptResponse(attempt.getPublicCode(),now,deadline(attempt),publicQuestions);
    }
    @Transactional
    public Api.MessageResponse saveAnswer(Api.AnswerRequest request,UserAccount user){
        ExamAttempt attempt=getAccessible(attempts.findByPublicCodeForUpdate(request.attemptCode())
                .orElseThrow(()->ApiException.notFound("Attempt not found")),user);
        if(attempt.getStatus()!=AttemptStatus.IN_PROGRESS) throw ApiException.conflict("Attempt is already submitted");
        Instant now=Instant.now();
        if(!now.isBefore(deadline(attempt))){finish(attempt,now,AttemptStatus.TIMED_OUT);return new Api.MessageResponse("Time expired; attempt was automatically submitted");}
        Question question=questionsForAttempt(attempt).stream().filter(q->q.getId().equals(request.questionId())).findFirst()
                .orElseThrow(()->ApiException.badRequest("Question is not part of this attempt"));
        String option=request.selectedOption().toUpperCase(Locale.ROOT);
        if(!bankService.readOptions(question).containsKey(option)) throw ApiException.badRequest("Selected option does not exist");
        Answer answer=answers.findByAttemptIdAndQuestionId(attempt.getId(),request.questionId())
                .orElseGet(()->new Answer(attempt,question,option));
        answer.setSelectedOption(option);answers.save(answer);
        return new Api.MessageResponse("Answer saved");
    }
    @Transactional
    public Api.ResultResponse submit(String code,UserAccount user){
        ExamAttempt attempt=getAccessible(attempts.findByPublicCodeForUpdate(code)
                .orElseThrow(()->ApiException.notFound("Attempt not found")),user);
        if(attempt.getStatus()==AttemptStatus.IN_PROGRESS){
            Instant now=Instant.now();AttemptStatus status=now.isBefore(deadline(attempt))?AttemptStatus.SUBMITTED:AttemptStatus.TIMED_OUT;
            finish(attempt,now,status);
        }
        return result(attempt);
    }
    @Transactional(readOnly=true)
    public Api.ResultResponse result(String code,UserAccount user){
        return result(getAccessible(code,user));
    }
    @Transactional
    public void releaseResults(UUID examId,UserAccount actor){
        Exam exam=exams.findById(examId).orElseThrow(()->ApiException.notFound("Exam not found"));
        if(Instant.now().isBefore(exam.getClosesAt())) throw ApiException.conflict("Results cannot be released before the exam closes");
        attempts.findByExamIdAndStatusForUpdate(examId,AttemptStatus.IN_PROGRESS).forEach(a->finish(a,Instant.now(),AttemptStatus.TIMED_OUT));
        exam.setStatus(ExamStatus.RESULTS_RELEASED);
        notifications.notifyAdmins("RESULTS_RELEASED","Results released for "+exam.getTitle());
        audit.record(actor,"RELEASE_RESULTS","Exam",examId.toString());
    }
    @Transactional
    public void releaseAttempt(UUID examId,UUID attemptId,UserAccount actor){
        Exam exam=exams.findById(examId).orElseThrow(()->ApiException.notFound("Exam not found"));
        if(Instant.now().isBefore(exam.getClosesAt())) throw ApiException.conflict("Results cannot be released before the exam closes");
        ExamAttempt attempt=attempts.findById(attemptId).orElseThrow(()->ApiException.notFound("Exam attempt not found"));
        if(!attempt.getExam().getId().equals(examId)) throw ApiException.badRequest("Attempt does not belong to this exam");
        if(attempt.getStatus()==AttemptStatus.IN_PROGRESS){
            if(attempt.remainingSeconds(Instant.now())>0) throw ApiException.conflict("An in-progress attempt cannot be released");
            finish(attempt,Instant.now(),AttemptStatus.TIMED_OUT);
        }
        boolean newlyReleased=!attempt.isReleased();
        attempt.release();
        if(newlyReleased && attempt.getStudent()!=null)
            notifications.notify(attempt.getStudent().getAccount(),"RESULT_RELEASED","Your examination result has been released.");
        else if(newlyReleased && schoolForAttempt(attempt)!=null)
            notifications.notify(schoolForAttempt(attempt).getAccount(),"RESULT_RELEASED",
                    "A participating student's examination result has been released.");
        audit.record(actor,"RELEASE_RESULT","ExamAttempt",attempt.getId().toString());
    }
    @Transactional
    public void releaseDistrictResults(UUID examId,UUID districtId,UserAccount actor){
        Exam exam=exams.findById(examId).orElseThrow(()->ApiException.notFound("Exam not found"));
        District district=districts.findById(districtId).orElseThrow(()->ApiException.notFound("District not found"));
        if(Instant.now().isBefore(exam.getClosesAt())) throw ApiException.conflict("Results cannot be released before the exam closes");
        if(exam.getDistrict()==null || !exam.getDistrict().getId().equals(districtId))
            throw ApiException.badRequest("The examination is not scheduled for this district");
        List<ExamAttempt> districtAttempts=attempts.findByExamIdOrderByStartedAtDesc(examId);
        Instant now=Instant.now();
        for(ExamAttempt attempt:districtAttempts){
            if(attempt.getStatus()==AttemptStatus.IN_PROGRESS){
                if(attempt.remainingSeconds(now)>0) throw ApiException.conflict("District has an exam attempt that is still in progress");
                finish(attempt,now,AttemptStatus.TIMED_OUT);
            }
        }
        if(!districtReleases.existsByExamIdAndDistrictId(examId,districtId))
            districtReleases.save(new ExamDistrictRelease(exam,district,actor));
        notifications.notifyAdmins("DISTRICT_RESULTS_RELEASED","Results released for "+exam.getTitle()+" in "+district.getName());
        audit.record(actor,"RELEASE_DISTRICT_RESULTS","ExamDistrict",examId+":"+districtId);
    }
    @Transactional
    public void configureLeaderboard(Api.PolicyRequest request,UserAccount actor){
        Set<String> allowed=Set.of("submission_time_asc","submission_time_desc","student_number_asc","student_number_desc");
        if(request.tieBreakOrder().stream().anyMatch(key->!allowed.contains(key))) throw ApiException.badRequest("Tie-break keys must be submission_time_asc, submission_time_desc, student_number_asc, or student_number_desc");
        if(new HashSet<>(request.tieBreakOrder()).size()!=request.tieBreakOrder().size()) throw ApiException.badRequest("Tie-break keys cannot be repeated");
        LeaderboardPolicy policy=policies.findById(1).orElseGet(LeaderboardPolicy::unconfigured);
        policy.configure(request.qualifiersPerDistrict(),String.join(",",request.tieBreakOrder()));policies.save(policy);
        audit.record(actor,"CONFIGURE_LEADERBOARD","LeaderboardPolicy","1");
    }
    @Transactional
    public Api.LeaderboardResponse publishLeaderboard(Api.LeaderboardPublishRequest request,UserAccount actor){
        Exam exam=exams.findById(request.examId()).orElseThrow(()->ApiException.notFound("Exam not found"));
        if(Instant.now().isBefore(exam.getClosesAt())) throw ApiException.conflict("The examination must be closed before publishing a leaderboard");
        if(request.schoolIds().stream().distinct().count()!=request.schoolIds().size())
            throw ApiException.badRequest("A school can only be selected once");
        if(request.schoolIds().isEmpty()) throw ApiException.badRequest("Select at least one school for the leaderboard");
        if(request.schoolIds().size()>20) throw ApiException.badRequest("A leaderboard may contain at most 20 schools");
        Set<UUID> eligibleSchools=attempts.findByExamIdOrderByStartedAtDesc(exam.getId()).stream()
                .filter(a->schoolForAttempt(a)!=null && a.getStatus()!=AttemptStatus.IN_PROGRESS && isResultReleased(a))
                .map(a->schoolForAttempt(a).getId()).collect(java.util.stream.Collectors.toSet());
        if(!eligibleSchools.containsAll(request.schoolIds())) throw ApiException.badRequest("Only schools with released, finalized student results can be published");
        LeaderboardPublication publication=leaderboardPublications.findById(exam.getId())
                .orElseGet(()->new LeaderboardPublication(exam.getId()));
        publication.publish(request.schoolIds());
        leaderboardPublications.save(publication);
        audit.record(actor,"PUBLISH_LEADERBOARD","Exam",exam.getId().toString());
        return publishedLeaderboard(exam.getId(),null);
    }
    @Transactional(readOnly=true)
    public Api.AdminLeaderboardResponse adminLeaderboard(UUID examId){
        List<Exam> available=exams.findAll().stream().sorted(Comparator.comparing(Exam::getClosesAt).reversed()).toList();
        Exam selected=examId==null?available.stream().filter(e->e.getStatus()==ExamStatus.RESULTS_RELEASED).findFirst().orElse(null)
                :exams.findById(examId).orElseThrow(()->ApiException.notFound("Exam not found"));
        if(selected==null) return new Api.AdminLeaderboardResponse(available.stream().map(this::response).toList(),
                null,List.of(),List.of(),false);
        Map<UUID,List<ExamAttempt>> bySchool=new HashMap<>();
        attempts.findByExamIdOrderByStartedAtDesc(selected.getId()).stream()
                .filter(a->schoolForAttempt(a)!=null && a.getStatus()!=AttemptStatus.IN_PROGRESS && isResultReleased(a))
                .forEach(a->bySchool.computeIfAbsent(schoolForAttempt(a).getId(),ignored->new ArrayList<>()).add(a));
        List<Api.LeaderboardSchoolCandidate> candidates=bySchool.entrySet().stream().map(entry->{
            School school=schoolForAttempt(entry.getValue().getFirst());
            BigDecimal best=entry.getValue().stream().map(ExamAttempt::getScore).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            return new Api.LeaderboardSchoolCandidate(school.getId(),school.getName(),school.getZone().getName(),
                    school.getZone().getDistrict().getName(),best,entry.getValue().size());
        }).sorted(Comparator.comparing(Api.LeaderboardSchoolCandidate::topScore).reversed()
                .thenComparing(Api.LeaderboardSchoolCandidate::name,String.CASE_INSENSITIVE_ORDER)).toList();
        LeaderboardPublication publication=leaderboardPublications.findById(selected.getId()).orElse(null);
        return new Api.AdminLeaderboardResponse(available.stream().map(this::response).toList(),selected.getId(),candidates,
                publication==null?List.of():publication.selectedSchoolIds(),publication!=null && publication.isPublished());
    }
    @Transactional(readOnly=true)
    private Api.LeaderboardResponse publishedLeaderboard(UUID examId,UUID districtId){
        LeaderboardPublication publication=leaderboardPublications.findById(examId).orElse(null);
        if(publication==null || !publication.isPublished())
            return new Api.LeaderboardResponse(examId,districtId,List.of(),false,false);
        Set<UUID> selected=new LinkedHashSet<>(publication.selectedSchoolIds());
        List<ExamAttempt> eligible=attempts.findByExamIdOrderByStartedAtDesc(examId).stream()
                .filter(a->schoolForAttempt(a)!=null && a.getStatus()!=AttemptStatus.IN_PROGRESS && isResultReleased(a)
                        && selected.contains(schoolForAttempt(a).getId()))
                .filter(a->districtId==null || Objects.equals(districtId(a),districtId))
                .sorted(Comparator.comparing(ExamAttempt::getScore).reversed().thenComparing(ExamAttempt::getSubmittedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<Api.LeaderboardEntry> entries=new ArrayList<>();
        int rank=0,index=0;
        while(index<eligible.size()){
            int end=index+1;
            while(end<eligible.size() && eligible.get(index).getScore().compareTo(eligible.get(end).getScore())==0) end++;
            rank=index+1;
            for(int i=index;i<end;i++){
                ExamAttempt attempt=eligible.get(i);School school=schoolForAttempt(attempt);
                entries.add(new Api.LeaderboardEntry(candidateName(attempt),school.getName(),school.getZone().getName(),
                        school.getZone().getDistrict().getName(),attempt.getScore(),rank,true));
            }
            index=end;
        }
        return new Api.LeaderboardResponse(examId,districtId,entries,false,publication.isPublished());
    }
    @Transactional(readOnly=true)
    public Api.LeaderboardResponse leaderboard(UUID examId,UUID districtId){
        Exam exam=exams.findById(examId).orElseThrow(()->ApiException.notFound("Exam not found"));
        if(exam.getDistrict()==null || !exam.getDistrict().getId().equals(districtId))
            throw ApiException.notFound("No examination schedule exists for this district");
        return publishedLeaderboard(examId,districtId);
    }
    private ExamAttempt getAccessible(String code,UserAccount user){
        return getAccessible(attempts.findByPublicCode(code).orElseThrow(()->ApiException.notFound("Attempt not found")),user);
    }
    private boolean isPlaceholderBank(QuestionBank bank){
        return bank.getName().equals("HSSC placeholder testing questions");
    }
    private ExamAttempt resolveAttempt(UUID examId,String code,UserAccount user){
        ExamAttempt attempt;
        if(code!=null && !code.isBlank()){
            attempt=attempts.findByPublicCode(code).orElseThrow(()->ApiException.notFound("Attempt not found"));
        }else{
            if(user==null) throw ApiException.badRequest("attemptCode is required for anonymous exam attempts");
            Student student=requireStudentIdentity(user);
            attempt=attempts.findByExamIdAndStudentIdForUpdate(examId,student.getId()).orElseThrow(()->ApiException.notFound("No attempt found for this exam"));
        }
        if(!attempt.getExam().getId().equals(examId)) throw ApiException.forbidden("Attempt belongs to another exam");
        return getAccessible(attempt,user);
    }
    private ExamAttempt resolveAttemptForUpdate(UUID examId,String code,UserAccount user){
        if(code!=null && !code.isBlank()){
            ExamAttempt attempt=attempts.findByPublicCodeForUpdate(code).orElseThrow(()->ApiException.notFound("Attempt not found"));
            if(!attempt.getExam().getId().equals(examId)) throw ApiException.forbidden("Attempt belongs to another exam");
            return getAccessible(attempt,user);
        }
        return resolveAttempt(examId,code,user);
    }
    private ExamAttempt getAccessible(ExamAttempt attempt,UserAccount user){
        if(attempt.getStudent()!=null){
            if(user==null || !isStudentIdentity(user) || !attempt.getStudent().getAccount().getId().equals(user.getId()))
                throw ApiException.forbidden("This attempt belongs to another account");
        } else if(attempt.getSchool()!=null){
            if(user==null || user.getRole()!=Role.SCHOOL && user.getRole()!=Role.ADMIN
                    || user.getRole()==Role.SCHOOL && !attempt.getSchool().getAccount().getId().equals(user.getId()))
                throw ApiException.forbidden("This attempt belongs to another school");
        } else if(attempt.getParticipant()!=null){
            if(user==null || user.getRole()!=Role.SCHOOL
                    || !attempt.getParticipant().getSchool().getAccount().getId().equals(user.getId()))
                throw ApiException.forbidden("This attempt belongs to another school");
        } else if(user!=null && !isStudentIdentity(user) && user.getRole()!=Role.ADMIN) throw ApiException.forbidden("Attempt access denied");
        return attempt;
    }
    private School schoolFor(UserAccount user){
        if(user==null || (user.getRole()!=Role.SCHOOL && user.getRole()!=Role.STUDENT))
            throw ApiException.forbidden("School account required");
        if(user.getRole()==Role.SCHOOL)
            return schools.findByAccountId(user.getId()).orElseThrow(()->ApiException.notFound("School profile not found"));
        return requireStudentIdentity(user).getSchool();
    }
    private SchoolParticipant requireParticipant(School school,UUID participantId){
        if(participantId==null) throw ApiException.badRequest("Select a participating student");
        SchoolParticipant participant=schoolParticipants.findById(participantId)
                .orElseThrow(()->ApiException.notFound("Participating student not found"));
        if(!participant.getSchool().getId().equals(school.getId()))
            throw ApiException.forbidden("Participating student belongs to another school");
        return participant;
    }
    private List<Question> questionsForAttempt(ExamAttempt attempt){
        List<ExamAttemptQuestion> saved=attemptQuestions.findByAttemptIdOrderByDisplayOrder(attempt.getId());
        if(!saved.isEmpty()) return saved.stream().map(ExamAttemptQuestion::getQuestion).toList();
        return assignments.findByExamIdOrderByDisplayOrder(attempt.getExam().getId()).stream()
                .map(ExamAssignment::getQuestion).limit(attempt.getExam().getQuestionCount()).toList();
    }
    private List<Api.PublicQuestion> publicQuestions(ExamAttempt attempt){
        List<Question> selected=questionsForAttempt(attempt);
        List<Api.PublicQuestion> response=new ArrayList<>();
        for(int i=0;i<selected.size();i++){
            Question question=selected.get(i);
            response.add(new Api.PublicQuestion(question.getId(),i+1,question.getPrompt(),question.getPoints(),
                    bankService.readOptions(question)));
        }
        return response;
    }
    private Student requireStudentIdentity(UserAccount user){
        if(user==null || !isStudentIdentity(user)) throw ApiException.forbidden("Student account required");
        return students.findByAccountId(user.getId()).orElseThrow(()->ApiException.notFound("Student profile not found"));
    }
    private boolean isStudentIdentity(UserAccount user){
        return user.getRole()==Role.STUDENT || user.getRole()==Role.SCHOOL && students.findByAccountId(user.getId()).isPresent();
    }
    private Instant deadline(ExamAttempt attempt){
        Instant personal=attempt.getStartedAt().plus(Duration.ofMinutes(attempt.getExam().getDurationMinutes()));
        return personal.isBefore(attempt.getExam().getClosesAt())?personal:attempt.getExam().getClosesAt();
    }
    private void finish(ExamAttempt attempt,Instant now,AttemptStatus status){
        Instant finalizedAt=status==AttemptStatus.TIMED_OUT && deadline(attempt).isBefore(now)?deadline(attempt):now;
        ResultStats stats=resultStats(attempt);
        attempt.finish(finalizedAt,status,stats.score(),stats.possible());
    }
    @Scheduled(fixedDelay=30000)
    @Transactional
    public void finalizeExpiredAttempts(){
        Instant now=Instant.now();
        attempts.findByStatusForUpdate(AttemptStatus.IN_PROGRESS).forEach(attempt->{
            if(attempt.remainingSeconds(now)==0) finalizeExpired(attempt,now);
        });
    }
    private void finalizeExpired(ExamAttempt attempt,Instant now){
        if(attempt.getStatus()==AttemptStatus.IN_PROGRESS && attempt.remainingSeconds(now)==0)
            finish(attempt,now,AttemptStatus.TIMED_OUT);
    }
    private Api.ResultResponse result(ExamAttempt attempt){
        UUID districtId=districtId(attempt);
        boolean released=isResultReleased(attempt);
        ResultStats stats=resultStats(attempt);
        return new Api.ResultResponse(attempt.getPublicCode(),attempt.getStatus().name(),candidateName(attempt),
                candidateId(attempt),attempt.getExam().getTitle(),released?stats.score():null,
                released?stats.possible():null,attempt.getSubmittedAt(),released,released?stats.total():null,released?stats.attempted():null,
                released?stats.attempted():null,released?stats.total()-stats.attempted():null,released?stats.correct():null,
                released?stats.attempted()-stats.correct():null,released?attempt.getExam().getMarksPerCorrectAnswer():null,
                released?stats.percentage():null,
                released?attempt.getStartedAt():null,released?stats.timeSpentSeconds():0,
                attempt.getStatus()==AttemptStatus.TIMED_OUT?"AUTOMATIC":"MANUAL");
    }
    private ResultStats resultStats(ExamAttempt attempt){
        List<Question> examQuestions=questionsForAttempt(attempt);
        List<Answer> attemptAnswers=answers.findByAttemptId(attempt.getId());
        Map<UUID,Answer> answerMap=new HashMap<>();
        attemptAnswers.forEach(answer->answerMap.put(answer.getQuestion().getId(),answer));
        List<ExamScoring.QuestionOutcome> outcomes=examQuestions.stream().map(question->{
            Answer answer=answerMap.get(question.getId());
            return new ExamScoring.QuestionOutcome(question.isAnonymous(),answer!=null,
                    answer!=null && question.isCorrect(answer.getSelectedOption()));
        }).toList();
        ExamScoring.Score scoring=ExamScoring.calculate(outcomes,attempt.getExam().getMarksPerCorrectAnswer());
        BigDecimal marksPerCorrect=attempt.getExam().getMarksPerCorrectAnswer();
        BigDecimal percentage=scoring.possible().signum()==0?BigDecimal.ZERO:
                scoring.score().multiply(BigDecimal.valueOf(100)).divide(scoring.possible(),2,RoundingMode.HALF_UP);
        long timeSpent=attempt.getSubmittedAt()==null?Math.max(0,Duration.between(attempt.getStartedAt(),Instant.now()).getSeconds())
                :Math.max(0,Duration.between(attempt.getStartedAt(),attempt.getSubmittedAt()).getSeconds());
        return new ResultStats(scoring.score(),scoring.possible(),scoring.total(),scoring.attempted(),scoring.correct(),
                scoring.anonymousBlankCredits(),percentage,timeSpent);
    }
    private boolean isResultReleased(ExamAttempt attempt){
        UUID districtId=districtId(attempt);
        return attempt.isReleased() || attempt.getExam().getStatus()==ExamStatus.RESULTS_RELEASED
                || districtId!=null && districtReleases.existsByExamIdAndDistrictId(attempt.getExam().getId(),districtId);
    }
    private UUID districtId(ExamAttempt attempt){
        School school=schoolForAttempt(attempt);
        return school==null?null:school.getZone().getDistrict().getId();
    }
    private School schoolForAttempt(ExamAttempt attempt){
        if(attempt.getStudent()!=null) return attempt.getStudent().getSchool();
        if(attempt.getSchool()!=null) return attempt.getSchool();
        if(attempt.getParticipant()!=null) return attempt.getParticipant().getSchool();
        return null;
    }
    private UUID candidateId(ExamAttempt attempt){
        if(attempt.getStudent()!=null) return attempt.getStudent().getId();
        if(!attempt.getTeamParticipants().isEmpty()) return attempt.getTeamParticipants().getFirst().getId();
        if(attempt.getParticipant()!=null) return attempt.getParticipant().getId();
        return null;
    }
    private String candidateName(ExamAttempt attempt){
        if(attempt.getStudent()!=null) return attempt.getStudent().getFullName();
        if(!attempt.getTeamParticipants().isEmpty())
            return attempt.getTeamParticipants().stream().map(SchoolParticipant::getFullName)
                    .collect(java.util.stream.Collectors.joining(" & "));
        if(attempt.getParticipant()!=null) return attempt.getParticipant().getFullName();
        return "Anonymous attempt";
    }
    @Transactional(readOnly=true)
    public List<Api.AdminExamResult> adminResults(UUID examId){
        return attempts.findAll().stream().filter(a->examId==null || a.getExam().getId().equals(examId))
                .sorted(Comparator.comparing(ExamAttempt::getStartedAt).reversed()).map(attempt->{
                    Api.ResultResponse result=result(attempt);
                    ResultStats stats=resultStats(attempt);
                    Student student=attempt.getStudent();
                    School school=schoolForAttempt(attempt);
                    return new Api.AdminExamResult(attempt.getExam().getId(),attempt.getExam().getTitle(),attempt.getId(),attempt.getPublicCode(),
                            candidateId(attempt),student==null?candidateName(attempt):student.getFullName(),
                            student==null?null:student.getStudentNumber(),school==null?null:school.getName(),
                            school==null?null:school.getZone().getDistrict().getName(),result.status(),
                            stats.score(),stats.possible(),attempt.getStartedAt(),attempt.getSubmittedAt(),
                            stats.timeSpentSeconds(),result.submissionType(),stats.total(),stats.attempted(),
                            stats.attempted(),stats.total()-stats.attempted(),stats.correct(),stats.attempted()-stats.correct(),
                            stats.anonymousBlankCredits(),
                            attempt.getExam().getMarksPerCorrectAnswer(),stats.percentage(),result.released());
                }).toList();
    }
    private Api.ExamResponse response(Exam exam){
        return new Api.ExamResponse(exam.getId(),exam.getTitle(),exam.getOpensAt(),exam.getClosesAt(),exam.getDurationMinutes(),
                exam.getQuestionCount(),exam.getStatus().name(),
                exam.getDistrict()==null?null:exam.getDistrict().getId(),exam.getDistrict()==null?null:exam.getDistrict().getName(),
                exam.getBank().getId(),exam.getBank().getName());
    }
    private record ResultStats(BigDecimal score,BigDecimal possible,int total,int attempted,int correct,
            int anonymousBlankCredits,
            BigDecimal percentage,long timeSpentSeconds) {}
}
