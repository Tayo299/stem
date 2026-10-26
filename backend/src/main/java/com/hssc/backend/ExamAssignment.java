package com.hssc.backend;
import jakarta.persistence.*;
@Entity @Table(name="exam_assignments", uniqueConstraints=@UniqueConstraint(name="uk_exam_question",columnNames={"exam_id","question_id"}))
public class ExamAssignment {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private java.util.UUID id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="exam_id",nullable=false) private Exam exam;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="question_id",nullable=false) private Question question;
    @Column(nullable=false) private int displayOrder;
    protected ExamAssignment() {}
    public ExamAssignment(Exam exam,Question question,int displayOrder){this.exam=exam;this.question=question;this.displayOrder=displayOrder;}
    public java.util.UUID getId(){return id;} public Exam getExam(){return exam;} public Question getQuestion(){return question;} public int getDisplayOrder(){return displayOrder;}
}
