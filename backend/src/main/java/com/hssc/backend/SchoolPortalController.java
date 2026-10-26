package com.hssc.backend;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/school")
public class SchoolPortalController {
    private final CurrentAccount current;
    private final SchoolRepository schools;
    private final SchoolParticipantRepository participants;

    public SchoolPortalController(CurrentAccount current, SchoolRepository schools,
            SchoolParticipantRepository participants) {
        this.current = current;
        this.schools = schools;
        this.participants = participants;
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public Api.SchoolDashboard dashboard(@AuthenticationPrincipal Jwt jwt) {
        UserAccount account = requireSchool(jwt);
        School school = schools.findByAccountId(account.getId())
                .orElseThrow(() -> ApiException.notFound("School profile not found"));
        Zone zone = school.getZone();
        District district = zone.getDistrict();
        List<Api.ParticipatingStudentResponse> roster = participants.findBySchoolIdOrderByFullNameAsc(school.getId())
                .stream()
                .map(student -> new Api.ParticipatingStudentResponse(student.getId(), student.getFullName(),
                        student.getGender(), student.getAge(), student.getGuardianName(), student.getGuardianPhone(), student.getHomeAddress(),
                        student.getPhoto().getId()))
                .toList();
        return new Api.SchoolDashboard(school.getId(), school.getName(), account.getEmail(),
                account.getStatus().name(), school.getAddress(), school.getRegistrationNumber(),
                school.getPrincipalName(), school.getPrincipalEmail(), school.getPrincipalPhone(),
                school.getTeacherInChargeName(), school.getTeacherInChargePhone(),
                school.getPrincipalSignatureDocumentId(), zone.getName(), district.getName(), roster);
    }

    private UserAccount requireSchool(Jwt jwt) {
        UserAccount account = current.get(jwt);
        current.requireActive(account);
        if (account.getRole() != Role.SCHOOL) throw ApiException.forbidden("School account required");
        return account;
    }
}
