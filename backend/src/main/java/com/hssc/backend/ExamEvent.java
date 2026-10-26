package com.hssc.backend;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="exam_events")
public class ExamEvent {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private java.util.UUID id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="attempt_id",nullable=false) private ExamAttempt attempt;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private Api.ExamEventReason reason;
    @Column(nullable=false,updatable=false) private Instant occurredAt=Instant.now();
    protected ExamEvent() {}
    public ExamEvent(ExamAttempt attempt,Api.ExamEventReason reason){this.attempt=attempt;this.reason=reason;}
}
