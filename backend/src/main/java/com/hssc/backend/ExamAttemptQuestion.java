package com.hssc.backend;

import jakarta.persistence.*;

@Entity
@Table(name="exam_attempt_questions", uniqueConstraints={
        @UniqueConstraint(name="uk_attempt_question",columnNames={"attempt_id","question_id"}),
        @UniqueConstraint(name="uk_attempt_question_order",columnNames={"attempt_id","display_order"})
})
public class ExamAttemptQuestion {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private java.util.UUID id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="attempt_id",nullable=false) private ExamAttempt attempt;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="question_id",nullable=false) private Question question;
    @Column(name="display_order",nullable=false) private int displayOrder;
    protected ExamAttemptQuestion() {}
    public ExamAttemptQuestion(ExamAttempt attempt,Question question,int displayOrder){
        this.attempt=attempt;this.question=question;this.displayOrder=displayOrder;
    }
    public java.util.UUID getId(){return id;}
    public ExamAttempt getAttempt(){return attempt;}
    public Question getQuestion(){return question;}
    public int getDisplayOrder(){return displayOrder;}
}
