package com.hssc.backend;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="notifications")
public class Notification {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private java.util.UUID id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="recipient_id",nullable=false) private UserAccount recipient;
    @Column(nullable=false,length=80) private String type;
    @Column(nullable=false,length=500) private String message;
    @Column(name="action_url",length=500) private String actionUrl;
    @Column(name="is_read",nullable=false) private boolean read;
    @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
    protected Notification() {}
    public Notification(UserAccount recipient,String type,String message,String actionUrl){this.recipient=recipient;this.type=type;this.message=message;this.actionUrl=actionUrl;}
    public java.util.UUID getId(){return id;} public String getType(){return type;} public String getMessage(){return message;} public boolean isRead(){return read;} public Instant getCreatedAt(){return createdAt;}
    public String getActionUrl(){return actionUrl;}
    public UserAccount getRecipient(){return recipient;}
    public void markRead(){read=true;}
}
