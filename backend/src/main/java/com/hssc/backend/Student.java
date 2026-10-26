package com.hssc.backend;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity @Table(name="students", uniqueConstraints=@UniqueConstraint(name="uk_student_school_number",columnNames={"school_id","student_number"}))
public class Student {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private java.util.UUID id;
    @OneToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="account_id",nullable=false,unique=true) private UserAccount account;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="school_id",nullable=false) private School school;
    @Column(nullable=false,length=30) private String studentNumber;
    @Column(nullable=false,length=100) private String firstName;
    @Column(nullable=false,length=100) private String lastName;
    @Column(name="full_name",nullable=false,length=200) private String fullName;
    @Column(name="age") private Integer age;
    @Column(length=40) private String gender;
    @Column(length=40) private String phone;
    @Column(name="parent_guardian_name",nullable=false,length=200) private String parentGuardianName;
    @Column(name="guardian_phone",nullable=false,length=40) private String guardianPhone;
    @Column(name="home_address",nullable=false,length=500) private String homeAddress;
    @Column(name="date_of_birth") private LocalDate dateOfBirth;
    protected Student() {}
    public Student(UserAccount account,School school,String studentNumber,String firstName,String lastName,String fullName,
            Integer age,String gender,String phone,String parentGuardianName,String guardianPhone,String homeAddress,LocalDate dob){
        this.account=account;this.school=school;this.studentNumber=studentNumber;this.firstName=firstName;this.lastName=lastName;
        this.fullName=fullName;this.age=age;this.gender=gender;this.phone=phone;this.parentGuardianName=parentGuardianName;
        this.guardianPhone=guardianPhone;
        this.homeAddress=homeAddress;this.dateOfBirth=dob;
    }
    public java.util.UUID getId(){return id;} public UserAccount getAccount(){return account;} public School getSchool(){return school;}
    public String getStudentNumber(){return studentNumber;} public String getFirstName(){return firstName;} public String getLastName(){return lastName;}
    public String getFullName(){return fullName;} public Integer getAge(){return age;} public String getGender(){return gender;}
    public String getPhone(){return phone;} public String getParentGuardianName(){return parentGuardianName;}
    public String getGuardianPhone(){return guardianPhone;}
    public String getHomeAddress(){return homeAddress;}
}
