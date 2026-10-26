package com.hssc.backend;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="answers",uniqueConstraints=@UniqueConstraint(name="uk_answer_attempt_question",columnNames={"attempt_id","question_id"}))
public class Answer {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private java.util.UUID id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="attempt_id",nullable=false) private ExamAttempt attempt;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="question_id",nullable=false) private Question question;
    @Column(nullable=false,length=1) private String selectedOption;
    @Column(nullable=false) private Instant updatedAt;
    protected Answer() {}
    public Answer(ExamAttempt attempt,Question question,String selectedOption){this.attempt=attempt;this.question=question;this.selectedOption=selectedOption;this.updatedAt=Instant.now();}
    public void setSelectedOption(String option){this.selectedOption=option;this.updatedAt=Instant.now();}
    public String getSelectedOption(){return selectedOption;} public Question getQuestion(){return question;}
}
