package com.hssc.backend;
import jakarta.persistence.*;
@Entity @Table(name="questions")
public class Question {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private java.util.UUID id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="bank_id",nullable=false) private QuestionBank bank;
    @Column(nullable=false,columnDefinition="text") private String prompt;
    @Column(nullable=false,length=80) private String subject;
    @Column(nullable=false) private int points;
    @Column(nullable=false,columnDefinition="text") private String optionsJson;
    @Column(length=1) private String correctOption;
    @Column(name="source_number") private Integer sourceNumber;
    @Column(name="anonymous",nullable=false) private boolean anonymous;
    protected Question() {}
    public Question(QuestionBank bank,String prompt,String subject,int points,String optionsJson,String correctOption){this(bank,prompt,subject,points,optionsJson,correctOption,null,false);}
    public Question(QuestionBank bank,String prompt,String subject,int points,String optionsJson,String correctOption,Integer sourceNumber){
        this(bank,prompt,subject,points,optionsJson,correctOption,sourceNumber,false);
    }
    public Question(QuestionBank bank,String prompt,String subject,int points,String optionsJson,String correctOption,Integer sourceNumber,boolean anonymous){
        this.bank=bank;this.prompt=prompt;this.subject=subject;this.points=points;this.optionsJson=optionsJson;this.correctOption=correctOption;this.sourceNumber=sourceNumber;this.anonymous=anonymous;
    }
    public java.util.UUID getId(){return id;} public QuestionBank getBank(){return bank;} public String getPrompt(){return prompt;} public String getSubject(){return subject;}
    public int getPoints(){return points;} public String getOptionsJson(){return optionsJson;} public String getCorrectOption(){return correctOption;}
    public Integer getSourceNumber(){return sourceNumber;}
    public boolean isAnonymous(){return anonymous;}
    public void update(String prompt,String subject,int points,String optionsJson,String correctOption,boolean anonymous){
        this.prompt=prompt;this.subject=subject;this.points=points;this.optionsJson=optionsJson;this.correctOption=correctOption;this.anonymous=anonymous;
    }
    public boolean isCorrect(String answer){return correctOption!=null && correctOption.equalsIgnoreCase(answer);}
}
