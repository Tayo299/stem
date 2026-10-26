package com.hssc.backend;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import jakarta.validation.Validator;
import java.util.List;

@RestController
@RequestMapping("/api")
public class RegistrationController {
    private final RegistrationService registrations; private final PortalService portal; private final Validator validator;
    public RegistrationController(RegistrationService registrations,PortalService portal,Validator validator){
        this.registrations=registrations;this.portal=portal;this.validator=validator;
    }
    @GetMapping("/districts") public List<Api.DistrictResponse> districts(){return registrations.districts();}
    @PostMapping("/registrations/schools") public Api.RegistrationResponse school(@Valid @RequestBody Api.SchoolRegistration request){
        if(!portal.enabled("registration")) throw ApiException.conflict("Registration is currently closed");return registrations.registerSchool(request);
    }
    @PostMapping("/registrations/students") public Api.RegistrationResponse student(@Valid @RequestBody Api.StudentRegistration request){
        if(!portal.enabled("registration")) throw ApiException.conflict("Registration is currently closed");return registrations.registerStudent(request);
    }
    @PostMapping(value="/registrations/schools/document",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Api.RegistrationResponse schoolWithDocument(@Valid @RequestPart("registration") Api.SchoolRegistration request,
            @RequestPart(value="document",required=false) MultipartFile document){
        if(!portal.enabled("registration")) throw ApiException.conflict("Registration is currently closed");return registrations.registerSchool(request,document);
    }
    @PostMapping(value="/registrations/students/document",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Api.RegistrationResponse studentWithDocument(@Valid @RequestPart("registration") Api.StudentRegistration request,
            @RequestPart("photo") MultipartFile photo){
        if(!portal.enabled("registration")) throw ApiException.conflict("Registration is currently closed");return registrations.registerStudent(request,photo,true);
    }
    @PostMapping(value="/schools/register",consumes=MediaType.APPLICATION_JSON_VALUE)
    public Api.RegistrationResponse registerSchoolJson(@Valid @RequestBody Api.SchoolRegistration request){
        if(!portal.enabled("registration")) throw ApiException.conflict("Registration is currently closed");return registrations.registerSchool(request);
    }
    @PostMapping(value="/schools/register",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Api.RegistrationResponse registerSchoolForm(@RequestParam Map<String,String> fields,
            @RequestPart(value="photo",required=false) MultipartFile photo,
            @RequestPart(value="document",required=false) MultipartFile document){
        if(!portal.enabled("registration")) throw ApiException.conflict("Registration is currently closed");
        MultipartFile proof=photo!=null?photo:document;
        return registrations.registerSchool(schoolForm(fields),proof);
    }
    @PostMapping(value="/schools/register/with-students",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Api.RegistrationResponse registerSchoolWithStudents(
            @Valid @RequestPart("registration") Api.SchoolRegistration request,
            @RequestPart("document") MultipartFile document,
            @RequestPart("participants") List<Api.ParticipatingStudentRegistration> participants,
            @RequestPart("studentPhotos") List<MultipartFile> studentPhotos){
        if(!portal.enabled("registration")) throw ApiException.conflict("Registration is currently closed");
        return registrations.registerSchool(request,document,participants,studentPhotos);
    }
    @PostMapping(value="/students/register",consumes=MediaType.APPLICATION_JSON_VALUE)
    public Api.RegistrationResponse registerStudentJson(@Valid @RequestBody Api.StudentRegistration request){
        if(!portal.enabled("registration")) throw ApiException.conflict("Registration is currently closed");return registrations.registerStudent(request);
    }
    @PostMapping(value="/students/register",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Api.RegistrationResponse registerStudentForm(@RequestParam Map<String,String> fields,
            @RequestPart(value="photo",required=false) MultipartFile photo,
            @RequestPart(value="document",required=false) MultipartFile document){
        if(!portal.enabled("registration")) throw ApiException.conflict("Registration is currently closed");
        return registrations.registerStudent(studentForm(fields),photo,true);
    }
    private Api.SchoolRegistration schoolForm(Map<String,String> f){
        String registrationNumber=f.get("registrationNumber");
        if(registrationNumber==null || registrationNumber.isBlank()) registrationNumber=f.get("schoolRegistrationNumber");
        return validate(new Api.SchoolRegistration(required(f,"email"),required(f,"password"),required(f,"confirmPassword"),first(f,"name","schoolName"),
                optional(registrationNumber),required(f,"address"),requiredUuid(f,"zoneId","zone"),
                required(f,"principalName"),required(f,"principalEmail"),required(f,"principalPhone"),
                required(f,"teacherInChargeName"),required(f,"teacherInChargePhone")));
    }
    private Api.StudentRegistration studentForm(Map<String,String> f){
        String dob=f.get("dateOfBirth"),age=f.get("age");
        LocalDate parsedDob=null;
        Integer parsedAge=null;
        try{
            if(dob!=null && !dob.isBlank()) parsedDob=LocalDate.parse(dob);
            if(age!=null && !age.isBlank()) parsedAge=Integer.valueOf(age);
        }catch(java.time.DateTimeException | NumberFormatException ex){
            throw ApiException.badRequest("Age and dateOfBirth must use valid values");
        }
        return validate(new Api.StudentRegistration(required(f,"email"),required(f,"password"),required(f,"confirmPassword"),first(f,"studentNumber","admissionNumber"),
                f.get("firstName"),f.get("lastName"),parsedDob,requiredUuid(f,"schoolId"),
                f.get("fullName"),parsedAge,f.get("gender"),required(f,"parentGuardianName"),
                required(f,"guardianPhone"),required(f,"homeAddress"),optional(f.get("phone"))));
    }
    private <T> T validate(T request){
        if(!validator.validate(request).isEmpty()) throw ApiException.badRequest("Registration fields are missing or invalid");
        return request;
    }
    private UUID requiredUuid(Map<String,String> fields,String... names){
        String value=first(fields,names);
        try{return UUID.fromString(value);}catch(IllegalArgumentException ex){
            throw ApiException.badRequest("Missing or invalid registration field: "+names[0]);
        }
    }
    private String required(Map<String,String> fields,String name){
        String value=fields.get(name);
        if(value==null || value.isBlank()) throw ApiException.badRequest("Missing required registration field: "+name);
        return value;
    }
    private String optional(String value){return value==null || value.isBlank()?null:value;}
    private String first(Map<String,String> fields,String... names){
        for(String name:names){
            String value=fields.get(name);
            if(value!=null && !value.isBlank()) return value;
        }
        throw ApiException.badRequest("Missing required registration field: "+names[0]);
    }
}
