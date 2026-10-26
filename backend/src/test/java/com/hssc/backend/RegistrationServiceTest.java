package com.hssc.backend;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegistrationServiceTest {
    @Test
    void schoolRegistrationPersistsSignatureAndUniformPhotosAgainstThatSchool() {
        UserRepository users = mock(UserRepository.class);
        ZoneRepository zones = mock(ZoneRepository.class);
        SchoolRepository schools = mock(SchoolRepository.class);
        SchoolParticipantRepository participants = mock(SchoolParticipantRepository.class);
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        NotificationService notifications = mock(NotificationService.class);
        AuditService audit = mock(AuditService.class);
        UploadService uploads = mock(UploadService.class);
        UUID zoneId = UUID.randomUUID();
        Zone zone = mock(Zone.class);
        School school = mock(School.class);
        DocumentUpload photo = mock(DocumentUpload.class);
        UUID signatureId = UUID.randomUUID();
        MockMultipartFile signature = new MockMultipartFile("document", "signature.png", "image/png",
                new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10});
        UUID schoolId = UUID.randomUUID();
        when(users.findByEmailIgnoreCase("principal@example.test")).thenReturn(Optional.empty());
        when(users.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(zones.findById(zoneId)).thenReturn(Optional.of(zone));
        when(zone.getId()).thenReturn(zoneId);
        when(schools.existsByNameIgnoreCaseAndZone_Id("Example School", zoneId)).thenReturn(false);
        when(schools.save(any(School.class))).thenReturn(school);
        when(school.getId()).thenReturn(schoolId);
        when(school.getName()).thenReturn("Example School");
        when(passwords.encode("a-strong-school-password")).thenReturn("encoded-password");
        when(uploads.uploadPhoto(any(), any())).thenReturn(
                new Api.UploadResponse(signatureId, "signature.png", "image/png", 8, "hash"));
        when(uploads.savePhoto(any(), any())).thenReturn(photo);
        RegistrationService service = new RegistrationService(users, mock(DistrictRepository.class), zones, schools,
                mock(StudentRepository.class), passwords, participants, notifications, audit, uploads);
        Api.SchoolRegistration request = new Api.SchoolRegistration("principal@example.test", "a-strong-school-password",
                "a-strong-school-password", "Example School", null, "School address", zoneId,
                "Principal Example", "principal@example.test", "1234567890", "Teacher Example", "1234567891");
        Api.ParticipatingStudentRegistration participant =
                new Api.ParticipatingStudentRegistration("Ada Lovelace", "Female", 16, "Parent Lovelace",
                        "+234 801 234 5678", "1 Science Street");
        MockMultipartFile uniformPhoto = new MockMultipartFile("studentPhotos", "ada.png", "image/png",
                new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10});

        Api.RegistrationResponse response = service.registerSchool(request, signature, List.of(participant), List.of(uniformPhoto));

        assertEquals("PENDING", response.status());
        var savedSchool = org.mockito.ArgumentCaptor.forClass(School.class);
        verify(schools).save(savedSchool.capture());
        assertEquals("Teacher Example", savedSchool.getValue().getTeacherInChargeName());
        assertEquals("1234567891", savedSchool.getValue().getTeacherInChargePhone());
        assertEquals(signatureId, savedSchool.getValue().getPrincipalSignatureDocumentId());
        verify(uploads).uploadPhoto(org.mockito.ArgumentMatchers.eq(signature),
                org.mockito.ArgumentMatchers.any(UserAccount.class));
        var savedParticipant = org.mockito.ArgumentCaptor.forClass(SchoolParticipant.class);
        verify(participants).save(savedParticipant.capture());
        assertSame(school, savedParticipant.getValue().getSchool());
        assertSame(photo, savedParticipant.getValue().getPhoto());
        assertEquals("Ada Lovelace", savedParticipant.getValue().getFullName());
        assertEquals("Female", savedParticipant.getValue().getGender());
        assertEquals(16, savedParticipant.getValue().getAge());
        assertEquals("Parent Lovelace", savedParticipant.getValue().getGuardianName());
        assertEquals("+234 801 234 5678", savedParticipant.getValue().getGuardianPhone());
        assertEquals("1 Science Street", savedParticipant.getValue().getHomeAddress());
        verify(uploads).savePhoto(org.mockito.ArgumentMatchers.eq(uniformPhoto),
                org.mockito.ArgumentMatchers.any(UserAccount.class));
    }

    @Test
    void successfulSchoolRegistrationRemainsPendingAndExplainsAdministratorApproval() {
        UserRepository users = mock(UserRepository.class);
        ZoneRepository zones = mock(ZoneRepository.class);
        SchoolRepository schools = mock(SchoolRepository.class);
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        UUID zoneId = UUID.randomUUID();
        Zone zone = mock(Zone.class);
        School school = mock(School.class);
        UUID schoolId = UUID.randomUUID();
        when(users.findByEmailIgnoreCase("principal@example.test")).thenReturn(Optional.empty());
        when(users.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(zones.findById(zoneId)).thenReturn(Optional.of(zone));
        when(zone.getId()).thenReturn(zoneId);
        when(schools.existsByNameIgnoreCaseAndZone_Id("Example School", zoneId)).thenReturn(false);
        when(schools.save(any(School.class))).thenReturn(school);
        when(school.getId()).thenReturn(schoolId);
        when(school.getName()).thenReturn("Example School");
        when(passwords.encode("a-strong-school-password")).thenReturn("encoded-password");
        RegistrationService service = new RegistrationService(users, mock(DistrictRepository.class), zones, schools,
                mock(StudentRepository.class), passwords, mock(SchoolParticipantRepository.class),
                mock(NotificationService.class), mock(AuditService.class),
                mock(UploadService.class));
        Api.SchoolRegistration request = new Api.SchoolRegistration("principal@example.test", "a-strong-school-password",
                "a-strong-school-password", "Example School", null, "School address", zoneId,
                "Principal Example", "principal@example.test", "1234567890", "Teacher Example", "1234567891");

        Api.RegistrationResponse response = service.registerSchool(request);

        assertEquals("PENDING", response.status());
        assertEquals("Registration successful. Please hold on. Your account will be reviewed and approved by the administrator before you can sign in.",
                response.message());
        var account = org.mockito.ArgumentCaptor.forClass(UserAccount.class);
        verify(users).save(account.capture());
        assertEquals(AccountStatus.PENDING, account.getValue().getStatus());
    }

    @Test
    void rejectsSchoolRegistrationWhenPasswordConfirmationDoesNotMatch() {
        UserRepository users = mock(UserRepository.class);
        RegistrationService service = new RegistrationService(users, mock(DistrictRepository.class), mock(ZoneRepository.class),
                mock(SchoolRepository.class), mock(StudentRepository.class), mock(PasswordEncoder.class),
                mock(SchoolParticipantRepository.class),
                mock(NotificationService.class), mock(AuditService.class), mock(UploadService.class));
        Api.SchoolRegistration request = new Api.SchoolRegistration("principal@example.test", "a-long-password-1",
                "a-different-password-2", "Example School", null, "Address", UUID.randomUUID(),
                "Principal", "principal@example.test", "1234567890", "Teacher", "1234567891");

        ApiException error = assertThrows(ApiException.class, () -> service.registerSchool(request));

        assertEquals("Password confirmation does not match", error.getMessage());
        verifyNoInteractions(users);
    }

    @Test
    void rejectsSchoolRegistrationWithMoreThanTwoParticipatingStudentsBeforeSaving() {
        UserRepository users = mock(UserRepository.class);
        RegistrationService service = new RegistrationService(users, mock(DistrictRepository.class), mock(ZoneRepository.class),
                mock(SchoolRepository.class), mock(StudentRepository.class), mock(PasswordEncoder.class),
                mock(SchoolParticipantRepository.class), mock(NotificationService.class), mock(AuditService.class),
                mock(UploadService.class));
        Api.SchoolRegistration request = new Api.SchoolRegistration("principal@example.test", "a-long-school-password",
                "a-long-school-password", "Example School", null, "Address", UUID.randomUUID(),
                "Principal", "principal@example.test", "1234567890", "Teacher", "1234567891");
        List<Api.ParticipatingStudentRegistration> overLimit = List.of(
                new Api.ParticipatingStudentRegistration("Student One", "Female", 16, "Parent One", "+234 801 111 1111", "1 Science Street"),
                new Api.ParticipatingStudentRegistration("Student Two", "Male", 17, "Parent Two", "+234 801 222 2222", "2 Science Street"),
                new Api.ParticipatingStudentRegistration("Student Three", "Female", 15, "Parent Three", "+234 801 333 3333", "3 Science Street"));

        ApiException error = assertThrows(ApiException.class,
                () -> service.registerSchool(request, null, overLimit, List.of()));

        assertEquals("Only two students may be registered per school at this time", error.getMessage());
        verifyNoInteractions(users);
    }

    @Test
    void registersFullNameAgeAndGenderAgainstTheSelectedActiveSchoolWithoutReturningCredentials() throws java.io.IOException {
        UserRepository users = mock(UserRepository.class);
        DistrictRepository districts = mock(DistrictRepository.class);
        ZoneRepository zones = mock(ZoneRepository.class);
        SchoolRepository schools = mock(SchoolRepository.class);
        StudentRepository students = mock(StudentRepository.class);
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        NotificationService notifications = mock(NotificationService.class);
        AuditService audit = mock(AuditService.class);
        UploadService uploads = mock(UploadService.class);

        UserAccount schoolAccount = new UserAccount("school@example.test", "hash", Role.SCHOOL, AccountStatus.ACTIVE);
        Zone zone = new Zone(new District("D1", "District One"), "Zone One");
        School school = new School(schoolAccount, zone, "Example School", "REG-1", "Address",
                "Principal", null, null, null, null, null);
        UUID schoolId = UUID.randomUUID();
        when(schools.findById(schoolId)).thenReturn(Optional.of(school));
        when(users.findByEmailIgnoreCase("student@example.test")).thenReturn(Optional.empty());
        when(students.existsByStudentNumberIgnoreCaseAndSchoolId("ST-1", schoolId)).thenReturn(false);
        when(passwords.encode("password-long-enough")).thenReturn("encoded-secret");
        when(users.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Student persistedStudent = mock(Student.class);
        when(persistedStudent.getId()).thenReturn(UUID.randomUUID());
        when(students.save(any(Student.class))).thenReturn(persistedStudent);

        RegistrationService service = new RegistrationService(users, districts, zones, schools, students, passwords,
                mock(SchoolParticipantRepository.class), notifications, audit, uploads);
        Api.StudentRegistration request = new Api.StudentRegistration("student@example.test", "password-long-enough",
                "password-long-enough", "ST-1", null, null, null, schoolId, "Ada Lovelace", 17, "Female",
                "Parent Lovelace", "+234 801 234 5678", "1 Science Street", null);
        org.springframework.web.multipart.MultipartFile photo = mock(org.springframework.web.multipart.MultipartFile.class);
        when(photo.isEmpty()).thenReturn(false);
        when(photo.getContentType()).thenReturn("image/png");
        when(photo.getSize()).thenReturn(8L);
        when(photo.getBytes()).thenReturn(new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10});

        Api.RegistrationResponse response = service.registerStudent(request, photo, true);

        assertEquals("PENDING", response.status());
        assertFalse(response.possibleDuplicate());
        Student savedStudent = captureSavedStudent(students);
        assertEquals("Ada Lovelace", savedStudent.getFullName());
        assertEquals(17, savedStudent.getAge());
        assertEquals("Female", savedStudent.getGender());
        assertEquals("Parent Lovelace", savedStudent.getParentGuardianName());
        assertEquals("+234 801 234 5678", savedStudent.getGuardianPhone());
        assertEquals("1 Science Street", savedStudent.getHomeAddress());
        assertSame(school, savedStudent.getSchool());
        assertFalse(response.toString().contains("password"));
        verify(uploads).uploadPhoto(any(org.springframework.web.multipart.MultipartFile.class), any(UserAccount.class));
    }

    private Student captureSavedStudent(StudentRepository students) {
        var captor = org.mockito.ArgumentCaptor.forClass(Student.class);
        verify(students, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }
}
