package com.hssc.backend;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class AdminService {
    private final UserRepository users; private final AuditService audit; private final NotificationService notifications;
    private final SchoolRepository schools; private final StudentRepository students; private final ExamRepository exams; private final ExamAttemptRepository attempts;
    private final DistrictRepository districts; private final ZoneRepository zones;
    public AdminService(UserRepository users,AuditService audit,NotificationService notifications,SchoolRepository schools,
            StudentRepository students,ExamRepository exams,ExamAttemptRepository attempts,DistrictRepository districts,ZoneRepository zones){
        this.users=users;this.audit=audit;this.notifications=notifications;this.schools=schools;this.students=students;this.exams=exams;this.attempts=attempts;
        this.districts=districts;this.zones=zones;
    }
    @Transactional(readOnly=true)
    public List<Api.AccountReview> pending(){
        return users.findByStatusOrderByCreatedAtAsc(AccountStatus.PENDING).stream()
                .map(u->new Api.AccountReview(u.getId(),u.getEmail(),u.getRole().name(),u.getStatus().name(),u.getCreatedAt())).toList();
    }
    @Transactional
    public Api.AccountReview setStatus(UUID id,AccountStatus status,UserAccount actor){
        UserAccount target=users.findById(id).orElseThrow(()->ApiException.notFound("Account not found"));
        if(target.getRole()==Role.ADMIN) throw ApiException.badRequest("Administrator status cannot be changed through this endpoint");
        target.setStatus(status);audit.record(actor,"ACCOUNT_STATUS_"+status,"Account",id.toString());
        notifications.notify(target,"ACCOUNT_REVIEW","Your account registration was "+status.name().toLowerCase(Locale.ROOT));
        return new Api.AccountReview(id,target.getEmail(),target.getRole().name(),target.getStatus().name(),target.getCreatedAt());
    }
    @Transactional(readOnly=true)
    public Map<String,Object> portalState(PortalService portal){
        return Map.of("registration",portal.enabled("registration"),"exam_access",portal.enabled("exam_access"),"result_review",portal.enabled("result_review"));
    }
    @Transactional(readOnly=true)
    public Api.AdminDashboard dashboard(){
        return new Api.AdminDashboard(users.countByStatus(AccountStatus.PENDING),
                schools.countByAccountStatus(AccountStatus.ACTIVE),students.countByAccountStatus(AccountStatus.ACTIVE),
                exams.count(),attempts.countByStatusNot(AttemptStatus.IN_PROGRESS));
    }
    @Transactional(readOnly=true)
    public List<Api.AdminDistrictSummary> districts(PortalService portal){
        return districts.findAll().stream().sorted(Comparator.comparing(District::getCode)).map(district->{
            List<Zone> districtZones=zones.findByDistrictId(district.getId());
            long schoolCount=0;
            long studentCount=0;
            for(Zone zone:districtZones){
                List<School> districtSchools=schools.findByZoneId(zone.getId());
                schoolCount+=districtSchools.size();
                for(School school:districtSchools) studentCount+=students.countBySchoolId(school.getId());
            }
            return new Api.AdminDistrictSummary(district.getId(),district.getCode(),district.getName(),
                    districtZones.size(),schoolCount,studentCount,portal.districtEnabled("exam_access",district.getId()));
        }).toList();
    }
}
