package com.hssc.backend;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api")
public class ExamController {
    private final CompetitionService competition; private final CurrentAccount current;
    private final SchoolRepository schools; private final StudentRepository students;
    public ExamController(CompetitionService competition,CurrentAccount current,SchoolRepository schools,StudentRepository students){
        this.competition=competition;this.current=current;this.schools=schools;this.students=students;
    }
    @GetMapping("/exams") public List<Api.ExamResponse> exams(@AuthenticationPrincipal Jwt jwt){
        return competition.listExamsFor(current.get(jwt));
    }
    @GetMapping("/exams/active") public List<Api.ExamResponse> activeExams(@AuthenticationPrincipal Jwt jwt){
        return competition.activeExamsFor(current.get(jwt));
    }
    @PostMapping("/exams/start") public Api.FrontendActiveExam startFrontend(@Valid @RequestBody(required=false) Api.FrontendStartRequest request,
            @AuthenticationPrincipal Jwt jwt){
        return competition.startForFrontend(request==null?null:request.examId(),jwt==null?null:current.get(jwt),
                request==null?null:request.participantId());
    }
    @PostMapping("/exams/{id}/start") public Api.FrontendActiveExam startFrontendExam(@PathVariable UUID id,
            @RequestBody(required=false) Api.FrontendStartRequest request,@AuthenticationPrincipal Jwt jwt){
        return competition.startFrontendExam(id,jwt==null?null:current.get(jwt),request==null?null:request.participantId());
    }
    @GetMapping("/exams/student/status") public Api.StudentExamStatus studentExamStatus(@AuthenticationPrincipal Jwt jwt){
        return competition.studentExamStatus(current.get(jwt));
    }
    @GetMapping("/exams/{id}/attempt") public Api.FrontendActiveExam currentAttempt(@PathVariable UUID id,
            @RequestParam(required=false) UUID participantId,@AuthenticationPrincipal Jwt jwt){
        return competition.frontendAttempt(id,current.get(jwt),participantId);
    }
    @GetMapping("/exams/{id}/time") public Api.ExamTimeResponse time(@PathVariable UUID id,@RequestParam(required=false) String attemptCode,
            @AuthenticationPrincipal Jwt jwt){
        return competition.time(id,attemptCode,jwt==null?null:current.get(jwt));
    }
    @PostMapping("/exams/{id}/events") public Api.MessageResponse event(@PathVariable UUID id,@Valid @RequestBody Api.ExamEventRequest request,
            @AuthenticationPrincipal Jwt jwt){
        return competition.recordExitEvent(id,request,jwt==null?null:current.get(jwt));
    }
    @PutMapping("/exams/{id}/answers/{questionId}") public Api.MessageResponse answerFrontend(@PathVariable UUID id,@PathVariable UUID questionId,
            @Valid @RequestBody Api.FrontendAnswerRequest request,@AuthenticationPrincipal Jwt jwt){
        return competition.saveAnswerForExam(id,questionId,request.selectedOption(),request.attemptCode(),jwt==null?null:current.get(jwt));
    }
    @PostMapping("/exams/{id}/submit") public Api.ResultResponse submitFrontend(@PathVariable UUID id,
            @RequestBody(required=false) Api.FrontendSubmitRequest request,@RequestParam(required=false) String attemptCode,
            @AuthenticationPrincipal Jwt jwt){
        String code=attemptCode!=null?attemptCode:request==null?null:request.attemptCode();
        return competition.submitForExam(id,code,jwt==null?null:current.get(jwt));
    }
    @PostMapping("/admin/exams") public Api.ExamResponse create(@Valid @RequestBody Api.ExamRequest request,@AuthenticationPrincipal Jwt jwt){
        return competition.createExam(request,current.get(jwt));
    }
    @GetMapping("/admin/exams") public List<Api.ExamResponse> adminExams(){return competition.listExams(true);}
    @PostMapping("/admin/exams/{id}/publish") public Api.ExamResponse publish(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){
        return competition.publish(id,current.get(jwt));
    }
    @PostMapping("/exams/{id}/attempts") public Api.AttemptResponse start(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){
        return competition.start(id,jwt==null?null:current.get(jwt));
    }
    @PostMapping("/exams/attempts/answers") public Api.MessageResponse answer(@Valid @RequestBody Api.AnswerRequest request,@AuthenticationPrincipal Jwt jwt){
        return competition.saveAnswer(request,jwt==null?null:current.get(jwt));
    }
    @PostMapping("/exams/attempts/submit") public Api.ResultResponse submit(@Valid @RequestBody Api.SubmitAttemptRequest request,@AuthenticationPrincipal Jwt jwt){
        return competition.submit(request.attemptCode(),jwt==null?null:current.get(jwt));
    }
    @GetMapping("/exams/attempts/{code}/result") public Api.ResultResponse result(@PathVariable String code,@AuthenticationPrincipal Jwt jwt){
        return competition.result(code,jwt==null?null:current.get(jwt));
    }
    @PostMapping("/admin/exams/{id}/release-results") public Api.MessageResponse release(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){
        competition.releaseResults(id,current.get(jwt));return new Api.MessageResponse("Results released");
    }
    @GetMapping("/admin/results") public List<Api.AdminExamResult> adminResults(@RequestParam(required=false) UUID examId){
        return competition.adminResults(examId);
    }
    @PostMapping("/admin/exams/{examId}/attempts/{attemptId}/release")
    public Api.MessageResponse releaseAttempt(@PathVariable UUID examId,@PathVariable UUID attemptId,@AuthenticationPrincipal Jwt jwt){
        competition.releaseAttempt(examId,attemptId,current.get(jwt));
        return new Api.MessageResponse("Student result released");
    }
    @GetMapping("/admin/leaderboard") public Api.AdminLeaderboardResponse adminLeaderboard(@RequestParam(required=false) UUID examId){
        return competition.adminLeaderboard(examId);
    }
    @PostMapping("/admin/leaderboard/publish") public Api.LeaderboardResponse publishLeaderboard(
            @Valid @RequestBody Api.LeaderboardPublishRequest request,@AuthenticationPrincipal Jwt jwt){
        return competition.publishLeaderboard(request,current.get(jwt));
    }
    @PostMapping("/admin/exams/{examId}/districts/{districtId}/release-results")
    public Api.MessageResponse releaseDistrict(@PathVariable UUID examId,@PathVariable UUID districtId,@AuthenticationPrincipal Jwt jwt){
        competition.releaseDistrictResults(examId,districtId,current.get(jwt));return new Api.MessageResponse("District results released");
    }
    @PutMapping("/admin/leaderboard-policy") public Api.MessageResponse policy(@Valid @RequestBody Api.PolicyRequest request,@AuthenticationPrincipal Jwt jwt){
        competition.configureLeaderboard(request,current.get(jwt));return new Api.MessageResponse("Leaderboard qualification policy saved");
    }
    @GetMapping("/admin/exams/{examId}/districts/{districtId}/leaderboard")
    public Api.LeaderboardResponse leaderboard(@PathVariable UUID examId,@PathVariable UUID districtId,@AuthenticationPrincipal Jwt jwt){
        current.get(jwt);
        return competition.leaderboard(examId,districtId);
    }
    @GetMapping("/districts/{districtId}/exams/{examId}/leaderboard")
    public Api.LeaderboardResponse districtLeaderboard(@PathVariable UUID examId,@PathVariable UUID districtId,@AuthenticationPrincipal Jwt jwt){
        UserAccount user=current.get(jwt);
        if(user.getRole()!=Role.ADMIN){
            UUID accountDistrict;
            if(user.getRole()==Role.SCHOOL && students.findByAccountId(user.getId()).isPresent()) accountDistrict=currentStudentDistrict(user);
            else if(user.getRole()==Role.SCHOOL) accountDistrict=currentSchoolDistrict(user);
            else if(user.getRole()==Role.STUDENT) accountDistrict=currentStudentDistrict(user);
            else throw ApiException.forbidden("District portal access denied");
            if(!accountDistrict.equals(districtId)) throw ApiException.forbidden("Leaderboard belongs to another district");
        }
        return competition.leaderboard(examId,districtId);
    }
    @GetMapping("/leaderboard") public Api.LeaderboardResponse frontendLeaderboard(@RequestParam(required=false) UUID examId,
            @RequestParam(required=false) UUID districtId,@AuthenticationPrincipal Jwt jwt){
        UserAccount user=current.get(jwt);
        UUID ownDistrict;
        if(user.getRole()==Role.STUDENT) ownDistrict=currentStudentDistrict(user);
        else if(user.getRole()==Role.SCHOOL && students.findByAccountId(user.getId()).isPresent()) ownDistrict=currentStudentDistrict(user);
        else if(user.getRole()==Role.SCHOOL) ownDistrict=currentSchoolDistrict(user);
        else if(user.getRole()==Role.ADMIN && districtId!=null) ownDistrict=districtId;
        else throw ApiException.badRequest("A districtId is required for administrators");
        if(districtId!=null && !ownDistrict.equals(districtId)) throw ApiException.forbidden("Leaderboard belongs to another district");
        return examId==null?competition.latestLeaderboard(ownDistrict):competition.leaderboard(examId,ownDistrict);
    }
    private UUID currentSchoolDistrict(UserAccount user){
        return schools.findByAccountId(user.getId()).orElseThrow(()->ApiException.notFound("School profile not found")).getZone().getDistrict().getId();
    }
    private UUID currentStudentDistrict(UserAccount user){
        return students.findByAccountId(user.getId()).orElseThrow(()->ApiException.notFound("Student profile not found")).getSchool().getZone().getDistrict().getId();
    }
}
