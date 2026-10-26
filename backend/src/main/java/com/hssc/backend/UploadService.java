package com.hssc.backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.core.io.Resource;

@Service
public class UploadService {
    private final DocumentRepository documents; private final AuditService audit; private final Path root;
    public UploadService(DocumentRepository documents,AuditService audit,@Value("${hssc.uploads.directory}") String directory){
        this.documents=documents;this.audit=audit;this.root=Path.of(directory).toAbsolutePath().normalize();
    }
    @Transactional
    public Api.UploadResponse upload(MultipartFile file,UserAccount owner){
        if(file==null || file.isEmpty()) throw ApiException.badRequest("A non-empty file is required");
        if(file.getSize()>5L*1024*1024) throw new ApiException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE,"File exceeds the 5 MB limit");
        String contentType=Optional.ofNullable(file.getContentType()).orElse("").toLowerCase(Locale.ROOT);
        String extension=switch(contentType){case "application/pdf"->".pdf";case "image/jpeg"->".jpg";case "image/png"->".png";default->throw ApiException.badRequest("Only PDF, JPEG, and PNG documents are accepted");};
        byte[] bytes;
        try{bytes=file.getBytes();}catch(IOException ex){throw ApiException.badRequest("Could not read uploaded document");}
        if(!validSignature(contentType,bytes)) throw ApiException.badRequest("File contents do not match the declared document type");
        String hash=sha256(bytes),key=hash+extension;
        try{
            Files.createDirectories(root);
            Path target=root.resolve(key).normalize();
            if(!target.getParent().equals(root)) throw ApiException.badRequest("Invalid storage path");
            if(!Files.exists(target)) Files.write(target,bytes,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
        }catch(FileAlreadyExistsException ignored){}catch(IOException ex){throw new IllegalStateException("Could not persist upload",ex);}
        String original=Path.of(Optional.ofNullable(file.getOriginalFilename()).orElse("document")).getFileName().toString();
        if(original.length()>180) original=original.substring(original.length()-180);
        DocumentUpload document=documents.save(new DocumentUpload(owner,key,original,contentType,file.getSize(),hash));
        audit.record(owner,"UPLOAD_DOCUMENT","DocumentUpload",document.getId().toString());
        return new Api.UploadResponse(document.getId(),original,contentType,file.getSize(),hash);
    }
    @Transactional
    public Api.UploadResponse uploadPhoto(MultipartFile file,UserAccount owner){
        String contentType=file==null?"":Optional.ofNullable(file.getContentType()).orElse("").toLowerCase(Locale.ROOT);
        if(!Set.of("image/jpeg","image/png").contains(contentType))
            throw ApiException.badRequest("Student photos must be JPEG or PNG images");
        return upload(file,owner);
    }
    @Transactional
    public DocumentUpload savePhoto(MultipartFile file,UserAccount owner){
        Api.UploadResponse saved=uploadPhoto(file,owner);
        return documents.findById(saved.id()).orElseThrow(()->new IllegalStateException("Uploaded photo record was not persisted"));
    }
    @Transactional(readOnly=true)
    public List<Api.DocumentMetadata> list(UserAccount owner){
        return documents.findByOwnerIdOrderByCreatedAtDesc(owner.getId()).stream().map(this::metadata).toList();
    }
    @Transactional(readOnly=true)
    public Api.DocumentMetadata metadata(UUID id){
        return documents.findById(id).map(this::metadata).orElseThrow(()->ApiException.notFound("Document not found"));
    }
    public ResponseEntity<Resource> download(UUID id){
        DocumentUpload document=documents.findById(id).orElseThrow(()->ApiException.notFound("Document not found"));
        Path path=root.resolve(document.getStorageKey()).normalize();
        if(!path.getParent().equals(root) || !Files.isRegularFile(path)) throw ApiException.notFound("Document content not found");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(document.getContentType()))
                .contentLength(document.getSize()).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"document\"")
                .body(new FileSystemResource(path));
    }
    public List<Api.DocumentMetadata> listForAccount(UUID ownerId){
        return documents.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(this::metadata).toList();
    }
    private Api.DocumentMetadata metadata(DocumentUpload document){
        return new Api.DocumentMetadata(document.getId(),document.getOwner().getId(),document.getOriginalName(),document.getContentType(),
                document.getSize(),document.getSha256(),document.getCreatedAt());
    }
    private boolean validSignature(String type,byte[] bytes){
        if(type.equals("application/pdf")) return bytes.length>=5 && new String(bytes,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-");
        if(type.equals("image/jpeg")) return bytes.length>=3 && (bytes[0]&255)==0xFF && (bytes[1]&255)==0xD8 && (bytes[2]&255)==0xFF;
        if(type.equals("image/png")) return bytes.length>=8 && Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10});
        return false;
    }
    private String sha256(byte[] bytes){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
}
