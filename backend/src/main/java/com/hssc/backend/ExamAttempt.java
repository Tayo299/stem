package com.hssc.backend;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
@Entity @Table(name="exam_attempts", uniqueConstraints=@UniqueConstraint(name="uk_exam_attempt_student",columnNames={"exam_id","student_id"}))
public class ExamAttempt {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private java.util.UUID id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="exam_id",nullable=false) private Exam exam;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="student_id") private Student student;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="school_participant_id") private SchoolParticipant participant;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="school_id") private School school;
    @ManyToMany(fetch=FetchType.LAZY)
    @JoinTable(name="exam_attempt_participants",joinColumns=@JoinColumn(name="attempt_id"),
            inverseJoinColumns=@JoinColumn(name="participant_id"))
    @OrderBy("fullName ASC")
    private List<SchoolParticipant> teamParticipants=new ArrayList<>();
    @Column(nullable=false,length=48,unique=true) private String publicCode;
    @Column(nullable=false) private Instant startedAt;
    private Instant submittedAt;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private AttemptStatus status=AttemptStatus.IN_PROGRESS;
    @Column(nullable=false,precision=8,scale=2) private BigDecimal score=BigDecimal.ZERO;
    @Column(nullable=false,precision=8,scale=2) private BigDecimal possiblePoints=BigDecimal.ZERO;
    @Column(nullable=false) private boolean released;
    protected ExamAttempt() {}
    public ExamAttempt(Exam exam,Student student,String publicCode,Instant startedAt){this(exam,student,null,publicCode,startedAt);}
    public ExamAttempt(Exam exam,Student student,SchoolParticipant participant,String publicCode,Instant startedAt){this(exam,student,participant,null,publicCode,startedAt);}
    public ExamAttempt(Exam exam,Student student,SchoolParticipant participant,School school,String publicCode,Instant startedAt){
        this.exam=exam;this.student=student;this.participant=participant;this.school=school;this.publicCode=publicCode;this.startedAt=startedAt;
    }
    public java.util.UUID getId(){return id;} public Exam getExam(){return exam;} public Student getStudent(){return student;}
    public SchoolParticipant getParticipant(){return participant;} public School getSchool(){return school;} public String getPublicCode(){return publicCode;}
    public List<SchoolParticipant> getTeamParticipants(){return teamParticipants;}
    public Instant getStartedAt(){return startedAt;} public Instant getSubmittedAt(){return submittedAt;} public AttemptStatus getStatus(){return status;}
    public BigDecimal getScore(){return score;} public BigDecimal getPossiblePoints(){return possiblePoints;}
    public boolean isReleased(){return released;} public void release(){this.released=true;}
    public int remainingSeconds(Instant now){
        if(status!=AttemptStatus.IN_PROGRESS)return 0;
        Instant end=startedAt.plus(Duration.ofMinutes(exam.getDurationMinutes())).isBefore(exam.getClosesAt())
                ?startedAt.plus(Duration.ofMinutes(exam.getDurationMinutes())):exam.getClosesAt();
        long millis=Math.max(0,Duration.between(now,end).toMillis());
        long seconds=(millis+999)/1000;
        return (int)Math.min(Integer.MAX_VALUE,seconds);
    }
    public void finish(Instant now,AttemptStatus status,BigDecimal score,BigDecimal possible){this.submittedAt=now;this.status=status;this.score=score;this.possiblePoints=possible;}
}
