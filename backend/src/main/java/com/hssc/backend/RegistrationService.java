package com.hssc.backend;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.*;

@Service
public class RegistrationService {
    private final UserRepository users; private final DistrictRepository districts; private final ZoneRepository zones;
    private final SchoolRepository schools; private final StudentRepository students; private final PasswordEncoder passwords;
    private final SchoolParticipantRepository participants; private final NotificationService notifications;
    private final AuditService audit; private final UploadService uploads;
    public RegistrationService(UserRepository users,DistrictRepository districts,ZoneRepository zones,SchoolRepository schools,
            StudentRepository students,PasswordEncoder passwords,SchoolParticipantRepository participants,
            NotificationService notifications,AuditService audit,UploadService uploads){
        this.users=users;this.districts=districts;this.zones=zones;this.schools=schools;this.students=students;
        this.passwords=passwords;this.participants=participants;this.notifications=notifications;this.audit=audit;this.uploads=uploads;
    }
    @Transactional(readOnly=true)
    public List<Api.DistrictResponse> districts(){
        return districts.findAll().stream().sorted(Comparator.comparing(District::getName)).map(d->new Api.DistrictResponse(
                d.getId(),d.getCode(),d.getName(),zones.findByDistrictId(d.getId()).stream().sorted(Comparator.comparing(Zone::getName))
                .map(z->new Api.ZoneResponse(z.getId(),z.getName())).toList())).toList();
    }
    @Transactional
    public Api.RegistrationResponse registerSchool(Api.SchoolRegistration request){
        return registerSchool(request,null);
    }
    @Transactional
    public Api.RegistrationResponse registerSchool(Api.SchoolRegistration request,MultipartFile document){
        return registerSchool(request,document,null,null);
    }
    @Transactional
    public Api.RegistrationResponse registerSchool(Api.SchoolRegistration request,MultipartFile document,
            List<Api.ParticipatingStudentRegistration> participatingStudents,List<MultipartFile> studentPhotos){
        if(!request.password().equals(request.confirmPassword()))
            throw ApiException.badRequest("Password confirmation does not match");
        boolean hasParticipants=participatingStudents!=null && !participatingStudents.isEmpty();
        if(participatingStudents!=null && participatingStudents.size()>2)
            throw ApiException.badRequest("Only two students may be registered per school at this time");
        if(hasParticipants && (studentPhotos==null || participatingStudents.size()!=studentPhotos.size()))
            throw ApiException.badRequest("Each participating student must have exactly one photograph");
        if(hasParticipants){
            for(Api.ParticipatingStudentRegistration participant:participatingStudents){
                if(participant==null || participant.fullName()==null || participant.fullName().isBlank()
                        || participant.fullName().length()>200 || participant.gender()==null || participant.gender().isBlank()
                        || participant.gender().length()>40 || participant.age()==null
                        || participant.age()<1 || participant.age()>120 || participant.guardianName()==null
                        || participant.guardianName().isBlank() || participant.guardianName().length()>200
                        || participant.guardianPhone()==null || !participant.guardianPhone().matches("^(?=(?:\\D*\\d){7,15}\\D*$)[+()0-9 .-]+$")
                        || participant.guardianPhone().length()>40
                        || participant.homeAddress()==null || participant.homeAddress().isBlank()
                        || participant.homeAddress().length()>500)
                    throw ApiException.badRequest("Each participating student must include valid name, gender, age, parent or guardian name and phone number, and home address");
            }
            if(studentPhotos.stream().anyMatch(photo->photo==null || photo.isEmpty()))
                throw ApiException.badRequest("Each participating student must include a photograph");
        }
        if(users.findByEmailIgnoreCase(request.email()).isPresent()) return duplicate("school",request.email());
        Zone zone=zones.findById(request.zoneId()).orElseThrow(()->ApiException.notFound("Zone not found"));
        String schoolName=request.name().trim();
        if(schools.existsByNameIgnoreCaseAndZone_Id(schoolName,zone.getId())) return duplicate("school",schoolName);
        String registrationNumber=trimToNull(request.registrationNumber());
        if(registrationNumber!=null && schools.existsByRegistrationNumberIgnoreCase(registrationNumber))
            return duplicate("school registration number",registrationNumber);
        UserAccount user=users.save(new UserAccount(request.email(),passwords.encode(request.password()),Role.SCHOOL,AccountStatus.PENDING));
        Api.UploadResponse signatureUpload = null;
        if(document!=null && !document.isEmpty()) signatureUpload = uploads.uploadPhoto(document,user);
        UUID signatureId = signatureUpload != null ? signatureUpload.id() : null;
        School school=schools.save(new School(user,zone,schoolName,registrationNumber,request.address().trim(),
                trimToNull(request.principalName()),trimToNull(request.principalEmail()),trimToNull(request.principalPhone()),
                trimToNull(request.teacherInChargeName()),trimToNull(request.teacherInChargePhone()),signatureId));
        if(hasParticipants){
            for(int i=0;i<participatingStudents.size();i++){
                Api.ParticipatingStudentRegistration participant=participatingStudents.get(i);
                DocumentUpload photo=uploads.savePhoto(studentPhotos.get(i),user);
                participants.save(new SchoolParticipant(school,photo,participant.fullName().trim(),
                        participant.gender().trim(),participant.age(),participant.guardianName().trim(),
                        participant.guardianPhone().trim(),participant.homeAddress().trim()));
            }
        }
        notifications.notifyAdmins("SCHOOL_REGISTRATION","School registration awaiting review: "+school.getName());
        audit.record(user,"REGISTER","School",school.getId().toString());
        return new Api.RegistrationResponse(school.getId(),"PENDING",false,
                "Registration successful. Please hold on. Your account will be reviewed and approved by the administrator before you can sign in.");
    }
    @Transactional
    public Api.RegistrationResponse registerStudent(Api.StudentRegistration request){
        return registerStudent(request,null);
    }
    @Transactional
    public Api.RegistrationResponse registerStudent(Api.StudentRegistration request,MultipartFile document){
        return registerStudent(request,document,false);
    }
    @Transactional
    public Api.RegistrationResponse registerStudent(Api.StudentRegistration request,MultipartFile document,boolean photo){
        if(!photo || document==null || document.isEmpty())
            throw ApiException.badRequest("A student photograph is required");
        if(!request.password().equals(request.confirmPassword()))
            throw ApiException.badRequest("Password confirmation does not match");
        String suppliedName=trimToNull(request.fullName());
        String firstName=trimToNull(request.firstName()),lastName=trimToNull(request.lastName());
        if(suppliedName==null && (firstName==null || lastName==null))
            throw ApiException.badRequest("Provide fullName or both firstName and lastName");
        if(suppliedName==null) suppliedName=(firstName+" "+lastName).trim();
        if(suppliedName.length()>200) throw ApiException.badRequest("fullName must not exceed 200 characters");
        if(request.age()==null && request.dateOfBirth()==null)
            throw ApiException.badRequest("Provide age or dateOfBirth");
        if(request.dateOfBirth()!=null && request.dateOfBirth().isAfter(LocalDate.now()))
            throw ApiException.badRequest("Date of birth cannot be in the future");
        if(request.age()!=null && (request.age()<1 || request.age()>120))
            throw ApiException.badRequest("Age must be between 1 and 120");
        if(firstName==null || lastName==null){
            int split=suppliedName.indexOf(' ');
            if(split<0){firstName=suppliedName;lastName="";}
            else{firstName=suppliedName.substring(0,split).trim();lastName=suppliedName.substring(split+1).trim();}
        }
        Integer age=request.age()!=null?request.age():request.dateOfBirth()==null?null:
                java.time.Period.between(request.dateOfBirth(),LocalDate.now()).getYears();
        if(age==null || age<1 || age>120) throw ApiException.badRequest("Age must be between 1 and 120");
        School school=schools.findById(request.schoolId()).orElseThrow(()->ApiException.notFound("School not found"));
        if(school.getAccount().getStatus()!=AccountStatus.ACTIVE) throw ApiException.conflict("The selected school is not active for student registration");
        String studentNumber=request.studentNumber().trim();
        if(users.findByEmailIgnoreCase(request.email()).isPresent() || students.existsByStudentNumberIgnoreCaseAndSchoolId(studentNumber,school.getId())){
            notifications.notify(school.getAccount(),"POSSIBLE_DUPLICATE","Possible duplicate student registration received for student number "+studentNumber);
            notifications.notifyAdmins("POSSIBLE_DUPLICATE","Possible duplicate student registration for school "+school.getName());
            return new Api.RegistrationResponse(null,"REJECTED",true,"Registration requires correction; the school was notified");
        }
        UserAccount user=users.save(new UserAccount(request.email(),passwords.encode(request.password()),Role.STUDENT,AccountStatus.PENDING));
        Student student=students.save(new Student(user,school,studentNumber,firstName,lastName,suppliedName,age,
                Optional.ofNullable(trimToNull(request.gender())).orElse("UNSPECIFIED"),trimToNull(request.phone()),
                request.parentGuardianName().trim(),request.guardianPhone().trim(),request.homeAddress().trim(),request.dateOfBirth()));
        uploads.uploadPhoto(document,user);
        notifications.notify(school.getAccount(),"STUDENT_REGISTRATION","Student registration awaiting school review: "+student.getFullName());
        audit.record(user,"REGISTER","Student",student.getId().toString());
        return new Api.RegistrationResponse(student.getId(),"PENDING",false,"Registration submitted for school review");
    }
    private String trimToNull(String value){return value==null || value.isBlank()?null:value.trim();}
    private Api.RegistrationResponse duplicate(String type,String identity){
        notifications.notifyAdmins("POSSIBLE_DUPLICATE","Possible duplicate "+type+" registration using "+identity);
        return new Api.RegistrationResponse(null,"REJECTED",true,"Registration requires correction; administrators were notified");
    }
}
