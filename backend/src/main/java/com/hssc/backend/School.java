package com.hssc.backend;
import jakarta.persistence.*;
@Entity @Table(name="schools")
public class School {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private java.util.UUID id;
    @OneToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="account_id",nullable=false,unique=true) private UserAccount account;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="zone_id",nullable=false) private Zone zone;
    @Column(nullable=false,length=180) private String name;
    @Column(length=80) private String registrationNumber;
    @Column(nullable=false,length=255) private String address;
    @Column(name="principal_name",length=180) private String principalName;
    @Column(name="principal_email",length=254) private String principalEmail;
    @Column(name="principal_phone",length=40) private String principalPhone;
    @Column(name="teacher_in_charge_name",length=180) private String teacherInChargeName;
    @Column(name="teacher_in_charge_phone",length=40) private String teacherInChargePhone;
    @Column(name="principal_signature_document_id") private java.util.UUID principalSignatureDocumentId;
    protected School() {}
    public School(UserAccount account,Zone zone,String name,String registrationNumber,String address,
            String principalName,String principalEmail,String principalPhone,
            String teacherInChargeName,String teacherInChargePhone,java.util.UUID principalSignatureDocumentId){
        this.account=account;this.zone=zone;this.name=name;this.registrationNumber=registrationNumber;this.address=address;
        this.principalName=principalName;this.principalEmail=principalEmail;this.principalPhone=principalPhone;
        this.teacherInChargeName=teacherInChargeName;this.teacherInChargePhone=teacherInChargePhone;this.principalSignatureDocumentId=principalSignatureDocumentId;
    }
    public java.util.UUID getId(){return id;} public UserAccount getAccount(){return account;} public Zone getZone(){return zone;}
    public String getName(){return name;} public String getRegistrationNumber(){return registrationNumber;}
    public String getAddress(){return address;} public String getPrincipalName(){return principalName;}
    public String getPrincipalEmail(){return principalEmail;} public String getPrincipalPhone(){return principalPhone;}
    public String getTeacherInChargeName(){return teacherInChargeName;} public String getTeacherInChargePhone(){return teacherInChargePhone;}
    public java.util.UUID getPrincipalSignatureDocumentId(){return principalSignatureDocumentId;} public void setPrincipalSignatureDocumentId(java.util.UUID id){this.principalSignatureDocumentId=id;}
}
