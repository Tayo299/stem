package com.hssc.backend;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.core.io.Resource;
import java.util.*;
@RestController @RequestMapping("/api/documents")
public class UploadController {
    private final UploadService uploads; private final CurrentAccount current; private final StudentRepository students;
    public UploadController(UploadService uploads,CurrentAccount current,StudentRepository students){
        this.uploads=uploads;this.current=current;this.students=students;
    }
    @PostMapping(consumes="multipart/form-data")
    public Api.UploadResponse upload(@RequestPart("file") MultipartFile file,@AuthenticationPrincipal Jwt jwt){
        UserAccount user=current.get(jwt);current.requireActive(user);return uploads.upload(file,user);
    }
    @GetMapping public List<Api.DocumentMetadata> list(@AuthenticationPrincipal Jwt jwt){
        UserAccount user=current.get(jwt);current.requireActive(user);return uploads.list(user);
    }
    @GetMapping("/{id}") public ResponseEntity<Resource> download(@PathVariable java.util.UUID id,@AuthenticationPrincipal Jwt jwt){
        UserAccount user=current.get(jwt);current.requireActive(user);
        Api.DocumentMetadata metadata=uploads.metadata(id);
        boolean owner=metadata.ownerId().equals(user.getId());
        boolean schoolOwnsStudent=user.getRole()==Role.SCHOOL && students.findByAccountId(metadata.ownerId())
                .map(student->student.getSchool().getAccount().getId().equals(user.getId())).orElse(false);
        if(user.getRole()!=Role.ADMIN && !owner && !schoolOwnsStudent) throw ApiException.forbidden("Document belongs to another account");
        return uploads.download(id);
    }
}
