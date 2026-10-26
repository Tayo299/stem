package com.hssc.backend;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/admin")
public class AdminController {
    private final AdminService admin; private final CurrentAccount current; private final PortalService portal; private final AuditService audit;
    private final UploadService uploads;
    private final RegistrationService registrations; private final ZoneRepository zones; private final DistrictRepository districts;
    private final SchoolDirectoryService schoolDirectory; private final StudentRepository students;
    private final SchoolParticipantRepository participants;
    public AdminController(AdminService admin,CurrentAccount current,PortalService portal,AuditService audit,UploadService uploads,
            RegistrationService registrations,ZoneRepository zones,DistrictRepository districts,SchoolDirectoryService schoolDirectory,
            StudentRepository students,SchoolParticipantRepository participants){
        this.admin=admin;this.current=current;this.portal=portal;this.audit=audit;this.uploads=uploads;
        this.registrations=registrations;this.zones=zones;this.districts=districts;this.schoolDirectory=schoolDirectory;
        this.students=students;this.participants=participants;
    }
    @GetMapping("/dashboard") public Api.AdminDashboard dashboard(){return admin.dashboard();}
    @GetMapping("/districts/overview") public List<Api.AdminDistrictSummary> districtOverview(){return admin.districts(portal);}
    @GetMapping("/accounts/pending") public List<Api.AccountReview> pending(){return admin.pending();}
    @PostMapping("/accounts/{id}/status") public Api.AccountReview status(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam AccountStatus status){
        return admin.setStatus(id,status,current.get(jwt));
    }
    @GetMapping("/portal") public Map<String,Object> portal(){return admin.portalState(portal);}
    @GetMapping("/audit") public List<Api.AuditRecord> audit(){return audit.latest();}
    @GetMapping("/accounts/{id}/documents") public List<Api.DocumentMetadata> documents(@PathVariable UUID id){return uploads.listForAccount(id);}
    @GetMapping("/districts") public List<Api.DistrictResponse> districts(){return registrations.districts();}
    @GetMapping("/districts/{districtId}/zones") public List<Api.ZoneResponse> zones(@PathVariable UUID districtId){
        if(!districts.existsById(districtId)) throw ApiException.notFound("District not found");
        return zones.findByDistrictId(districtId).stream().sorted(Comparator.comparing(Zone::getName))
                .map(z->new Api.ZoneResponse(z.getId(),z.getName())).toList();
    }
    @GetMapping("/zones/{zoneId}/schools") public List<Api.SchoolAdminDetail> zoneSchools(@PathVariable UUID zoneId){
        if(!zones.existsById(zoneId)) throw ApiException.notFound("Zone not found");
        return schoolDirectory.adminSchools(zoneId);
    }
    @GetMapping("/schools") public List<Api.SchoolAdminDetail> schools(@RequestParam(required=false) UUID zoneId){
        return schoolDirectory.adminSchools(zoneId);
    }
    @GetMapping("/schools/{id}") public Api.SchoolAdminDetail school(@PathVariable UUID id){return schoolDirectory.adminSchool(id);}
    @GetMapping("/schools/{id}/students") public List<Api.AdminStudentDetail> schoolStudents(@PathVariable UUID id){
        schoolDirectory.adminSchool(id);
        return students.findBySchoolId(id).stream().sorted(Comparator.comparing(Student::getFullName,String.CASE_INSENSITIVE_ORDER))
                .map(student->new Api.AdminStudentDetail(student.getId(),student.getAccount().getEmail(),student.getFullName(),
                        student.getStudentNumber(),student.getAge(),student.getGender(),student.getPhone(),
                        student.getParentGuardianName(),student.getGuardianPhone(),student.getHomeAddress(),student.getAccount().getStatus().name(),
                        uploads.listForAccount(student.getAccount().getId()))).toList();
    }
    @GetMapping("/schools/{id}/participants") @Transactional(readOnly=true)
    public List<Api.ParticipatingStudentResponse> schoolParticipants(@PathVariable UUID id){
        schoolDirectory.adminSchool(id);
        return participants.findBySchoolIdOrderByFullNameAsc(id).stream()
                .map(student->new Api.ParticipatingStudentResponse(student.getId(),student.getFullName(),student.getGender(),
                        student.getAge(),student.getGuardianName(),student.getGuardianPhone(),student.getHomeAddress(),student.getPhoto().getId())).toList();
    }
    @DeleteMapping("/schools/{id}") public Api.MessageResponse deleteSchool(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
        schoolDirectory.archive(id,current.get(jwt));return new Api.MessageResponse("School and associated student accounts were disabled; competition history was retained");
    }
    @PutMapping("/portal/{key}") public Api.MessageResponse portal(@AuthenticationPrincipal Jwt jwt,@PathVariable String key,@Valid @RequestBody Api.PortalRequest request){
        portal.set(key,request.enabled(),current.get(jwt));return new Api.MessageResponse("Portal control updated");
    }
    @PutMapping("/districts/{districtId}/portal/{key}") public Api.MessageResponse districtPortal(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID districtId,@PathVariable String key,@Valid @RequestBody Api.PortalRequest request){
        portal.setDistrict(key,districtId,request.enabled(),current.get(jwt));return new Api.MessageResponse("District portal control updated");
    }
}
