package com.hssc.backend;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api")
public class AccountController {
    private final CurrentAccount current; private final NotificationService notifications;
    private final SchoolRepository schools; private final StudentRepository students; private final AuditService audit;
    private final CompetitionService competition; private final UploadService uploads; private final PortalService portal;
    public AccountController(CurrentAccount current,NotificationService notifications,SchoolRepository schools,StudentRepository students,
            AuditService audit,CompetitionService competition,UploadService uploads,PortalService portal){
        this.current=current;this.notifications=notifications;this.schools=schools;this.students=students;this.audit=audit;
        this.competition=competition;this.uploads=uploads;this.portal=portal;
    }
    @GetMapping("/me") public Map<String,Object> me(@AuthenticationPrincipal Jwt jwt){
        UserAccount user=current.get(jwt);current.requireActive(user);
        Map<String,Object> response=new LinkedHashMap<>();response.put("id",user.getId());response.put("email",user.getEmail());response.put("role",user.getRole().name());
        if(user.getRole()==Role.SCHOOL) schools.findByAccountId(user.getId()).ifPresent(s->{response.put("schoolId",s.getId());response.put("schoolName",s.getName());});
        students.findByAccountId(user.getId()).ifPresent(s->{response.put("studentId",s.getId());response.put("schoolId",s.getSchool().getId());response.put("studentName",s.getFullName());});
        return response;
    }
    @GetMapping("/student/dashboard") public Api.StudentDashboard studentDashboard(@AuthenticationPrincipal Jwt jwt){
        UserAccount user=current.get(jwt);
        Student student=requireStudentProfile(user);
        List<Api.ExamResponse> active=competition.activeExamsFor(user);
        List<Api.ResultResponse> results=competition.studentResults(student);
        String examStatus=results.stream().findFirst().map(Api.ResultResponse::status).orElse("NOT_STARTED");
        String resultStatus=results.stream().anyMatch(Api.ResultResponse::released)?"AVAILABLE":
                results.isEmpty()?"NOT_RELEASED":"UNDER_REVIEW";
        return new Api.StudentDashboard(student.getId(),student.getFullName(),
                student.getSchool().getName(),user.getStatus().name(),
                portal.districtEnabled("exam_access",student.getSchool().getZone().getDistrict().getId())?"OPEN":"LOCKED",
                examStatus,resultStatus,"PENDING",active,results);
    }
    @GetMapping("/results/me") public List<Api.ResultResponse> results(@AuthenticationPrincipal Jwt jwt){
        UserAccount user=current.get(jwt);
        current.requireActive(user);
        return competition.resultsForAccount(user);
    }
    @PostMapping("/school/student-profile") public Api.MessageResponse createSchoolHolderStudentProfile(
            @Valid @RequestBody Api.SchoolStudentProfileRequest request,@AuthenticationPrincipal Jwt jwt){
        UserAccount user=current.get(jwt);
        if(user.getRole()!=Role.SCHOOL) throw ApiException.forbidden("School account required");
        School school=schools.findByAccountId(user.getId()).orElseThrow(()->ApiException.notFound("School profile not found"));
        if(students.findByAccountId(user.getId()).isPresent())
            throw ApiException.conflict("Your school account already has a student profile");
        if(students.existsByStudentNumberIgnoreCaseAndSchoolId(request.studentNumber().trim(),school.getId()))
            throw ApiException.conflict("This student number is already registered at the school");
        String fullName=request.fullName().trim();
        int split=fullName.indexOf(' ');
        String firstName=split<0?fullName:fullName.substring(0,split).trim();
        String lastName=split<0?"":fullName.substring(split+1).trim();
        students.save(new Student(user,school,request.studentNumber().trim(),firstName,lastName,fullName,
                request.age(),request.gender().trim(),null,"","","",null));
        audit.record(user,"CREATE_SCHOOL_HOLDER_STUDENT_PROFILE","Student",user.getId().toString());
        return new Api.MessageResponse("Student profile created for the school account holder");
    }
    @GetMapping("/notifications") public List<Api.NotificationResponse> notifications(@AuthenticationPrincipal Jwt jwt){
        UserAccount user=current.get(jwt);current.requireActive(user);return notifications.forAccount(user);
    }
    @PostMapping("/notifications/{id}/read") public Api.MessageResponse read(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
        UserAccount user=current.get(jwt);current.requireActive(user);notifications.markRead(user,id);return new Api.MessageResponse("Notification marked as read");
    }
    private Student requireStudentProfile(UserAccount user){
        if(user.getRole()!=Role.STUDENT && user.getRole()!=Role.SCHOOL)
            throw ApiException.forbidden("Student access required");
        return students.findByAccountId(user.getId()).orElseThrow(()->ApiException.notFound(
                user.getRole()==Role.SCHOOL?"Complete your school account holder student profile first":"Student profile not found"));
    }
    @GetMapping("/school/students") public List<Api.StudentSummary> schoolStudents(@AuthenticationPrincipal Jwt jwt){
        UserAccount user=current.get(jwt);current.requireActive(user);
        if(user.getRole()!=Role.SCHOOL) throw ApiException.forbidden("School account required");
        School school=schools.findByAccountId(user.getId()).orElseThrow(()->ApiException.notFound("School profile not found"));
        return students.findBySchoolId(school.getId()).stream().map(s->new Api.StudentSummary(s.getId(),s.getStudentNumber(),
                s.getFirstName(),s.getLastName(),s.getAccount().getStatus().name(),s.getFullName(),s.getAge(),s.getGender())).toList();
    }
    @GetMapping("/school/students/{id}/documents") public List<Api.DocumentMetadata> schoolStudentDocuments(
            @AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
        UserAccount user=current.get(jwt);current.requireActive(user);
        if(user.getRole()!=Role.SCHOOL) throw ApiException.forbidden("School account required");
        School school=schools.findByAccountId(user.getId()).orElseThrow(()->ApiException.notFound("School profile not found"));
        Student student=students.findById(id).orElseThrow(()->ApiException.notFound("Student not found"));
        if(!student.getSchool().getId().equals(school.getId())) throw ApiException.forbidden("Student belongs to another school");
        return uploads.listForAccount(student.getAccount().getId());
    }
    @PostMapping("/school/students/{id}/status") public Api.AccountReview setStudentStatus(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam AccountStatus status){
        UserAccount user=current.get(jwt);current.requireActive(user);
        if(user.getRole()!=Role.SCHOOL || status==AccountStatus.PENDING || status==AccountStatus.DISABLED)
            throw ApiException.forbidden("School accounts may activate or reject their pending students");
        School school=schools.findByAccountId(user.getId()).orElseThrow(()->ApiException.notFound("School profile not found"));
        Student student=students.findById(id).orElseThrow(()->ApiException.notFound("Student not found"));
        if(!student.getSchool().getId().equals(school.getId())) throw ApiException.forbidden("Student belongs to another school");
        student.getAccount().setStatus(status);
        notifications.notify(student.getAccount(),"ACCOUNT_REVIEW","Your school registration review was "+status.name().toLowerCase(Locale.ROOT));
        audit.record(user,"STUDENT_STATUS_"+status,"Student",student.getId().toString());
        return new Api.AccountReview(student.getAccount().getId(),student.getAccount().getEmail(),Role.STUDENT.name(),status.name(),student.getAccount().getCreatedAt());
    }
}
