package com.hssc.backend;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="question_banks")
public class QuestionBank {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private java.util.UUID id;
    @Column(nullable=false,length=120) private String name;
    @Column(nullable=false) private boolean active;
    @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="created_by") private UserAccount createdBy;
    protected QuestionBank() {}
    public QuestionBank(String name,UserAccount creator){this.name=name;this.createdBy=creator;this.active=true;}
    public java.util.UUID getId(){return id;} public String getName(){return name;} public boolean isActive(){return active;} public void setActive(boolean active){this.active=active;}
}
