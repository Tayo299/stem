package com.hssc.backend;
import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
@Entity @Table(name="exams")
public class Exam {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private java.util.UUID id;
    @Column(nullable=false,length=160) private String title;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="bank_id",nullable=false) private QuestionBank bank;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="district_id") private District district;
    @Column(nullable=false) private Instant opensAt;
    @Column(nullable=false) private Instant closesAt;
    @Column(nullable=false) private int durationMinutes;
    @Column(nullable=false) private int questionCount;
    @Column(name="marks_per_correct_answer",nullable=false,precision=8,scale=2) private BigDecimal marksPerCorrectAnswer;
    @Column(nullable=false) private boolean anonymousAllowed;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private ExamStatus status=ExamStatus.DRAFT;
    protected Exam() {}
    public Exam(String title,QuestionBank bank,District district,Instant opensAt,Instant closesAt,int duration,int questionCount,BigDecimal marksPerCorrectAnswer,boolean anonymousAllowed){this.title=title;this.bank=bank;this.district=district;this.opensAt=opensAt;this.closesAt=closesAt;this.durationMinutes=duration;this.questionCount=questionCount;this.marksPerCorrectAnswer=marksPerCorrectAnswer;this.anonymousAllowed=anonymousAllowed;}
    public java.util.UUID getId(){return id;} public String getTitle(){return title;} public QuestionBank getBank(){return bank;} public District getDistrict(){return district;} public Instant getOpensAt(){return opensAt;}
    public Instant getClosesAt(){return closesAt;} public int getDurationMinutes(){return durationMinutes;} public int getQuestionCount(){return questionCount;}
    public BigDecimal getMarksPerCorrectAnswer(){return marksPerCorrectAnswer;}
    public boolean isAnonymousAllowed(){return anonymousAllowed;} public ExamStatus getStatus(){return status;} public void setStatus(ExamStatus status){this.status=status;}
}
