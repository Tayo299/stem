package com.hssc.backend;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="document_uploads")
public class DocumentUpload {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private java.util.UUID id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="owner_id",nullable=false) private UserAccount owner;
    @Column(nullable=false,length=128) private String storageKey;
    @Column(nullable=false,length=180) private String originalName;
    @Column(nullable=false,length=100) private String contentType;
    @Column(nullable=false) private long size;
    @Column(nullable=false,length=64) private String sha256;
    @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
    protected DocumentUpload() {}
    public DocumentUpload(UserAccount owner,String key,String name,String type,long size,String hash){this.owner=owner;this.storageKey=key;this.originalName=name;this.contentType=type;this.size=size;this.sha256=hash;}
    public java.util.UUID getId(){return id;}
    public UserAccount getOwner(){return owner;} public String getStorageKey(){return storageKey;} public String getOriginalName(){return originalName;}
    public String getContentType(){return contentType;} public long getSize(){return size;} public String getSha256(){return sha256;} public Instant getCreatedAt(){return createdAt;}
}
