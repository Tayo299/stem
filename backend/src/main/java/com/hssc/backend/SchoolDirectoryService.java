package com.hssc.backend;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class SchoolDirectoryService {
    private final SchoolRepository schools;
    private final StudentRepository students;
    private final AuditService audit;

    public SchoolDirectoryService(SchoolRepository schools, StudentRepository students,
            AuditService audit) {
        this.schools = schools;
        this.students = students;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<Api.SchoolDirectoryEntry> eligibleSchools() {
        return schools.findAll().stream()
                .filter(s -> s.getAccount().getRole() == Role.SCHOOL && s.getAccount().getStatus() == AccountStatus.ACTIVE)
                .map(this::directoryEntry)
                .sorted(Comparator.comparing(Api.SchoolDirectoryEntry::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Api.SchoolAdminDetail> adminSchools(UUID zoneId) {
        return schools.findAll().stream().filter(s -> zoneId == null || s.getZone().getId().equals(zoneId))
                .sorted(Comparator.comparing(School::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::adminDetail).toList();
    }

    @Transactional(readOnly = true)
    public Api.SchoolAdminDetail adminSchool(UUID id) {
        return adminDetail(schools.findById(id).orElseThrow(() -> ApiException.notFound("School not found")));
    }

    @Transactional
    public void archive(UUID id, UserAccount actor) {
        School school = schools.findById(id).orElseThrow(() -> ApiException.notFound("School not found"));
        school.getAccount().setStatus(AccountStatus.DISABLED);
        students.findBySchoolId(id).forEach(student -> {
            student.getAccount().setStatus(AccountStatus.DISABLED);
            audit.record(actor, "SCHOOL_ARCHIVE_STUDENT", "Student", student.getId().toString());
        });
        audit.record(actor, "ARCHIVE", "School", id.toString());
    }

    private Api.SchoolDirectoryEntry directoryEntry(School school) {
        Zone zone = school.getZone();
        District district = zone.getDistrict();
        return new Api.SchoolDirectoryEntry(school.getId(), school.getName(), school.getRegistrationNumber(),
                school.getAddress(), zone.getId(), zone.getName(), district.getId(), district.getName(), district.getCode());
    }

    private Api.SchoolAdminDetail adminDetail(School school) {
        Zone zone = school.getZone();
        District district = zone.getDistrict();
        return new Api.SchoolAdminDetail(school.getId(), school.getAccount().getId(), school.getName(), school.getRegistrationNumber(),
                school.getAddress(), school.getAccount().getStatus().name(), school.getAccount().getEmail(),
                school.getPrincipalName(), school.getPrincipalEmail(), school.getPrincipalPhone(),
                school.getTeacherInChargeName(), school.getTeacherInChargePhone(), school.getPrincipalSignatureDocumentId(),
                zone.getId(), zone.getName(), district.getId(), district.getName(), students.countBySchoolId(school.getId()));
    }
}
