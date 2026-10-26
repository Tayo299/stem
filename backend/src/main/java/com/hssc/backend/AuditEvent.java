package com.hssc.backend;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="audit_events")
public class AuditEvent {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private java.util.UUID id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="actor_id") private UserAccount actor;
    @Column(nullable=false,length=100) private String action;
    @Column(nullable=false,length=100) private String entityType;
    @Column(length=64) private String entityId;
    @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
    protected AuditEvent() {}
    public AuditEvent(UserAccount actor,String action,String entityType,String entityId){this.actor=actor;this.action=action;this.entityType=entityType;this.entityId=entityId;}
    public java.util.UUID getId(){return id;} public UserAccount getActor(){return actor;} public String getAction(){return action;}
    public String getEntityType(){return entityType;} public String getEntityId(){return entityId;} public Instant getCreatedAt(){return createdAt;}
}
