package com.hssc.backend;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "school_participants")
public class SchoolParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_document_id", nullable = false, unique = true)
    private DocumentUpload photo;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(nullable = false, length = 40)
    private String gender;

    @Column(nullable = false)
    private int age;

    @Column(name = "guardian_name", nullable = false, length = 200)
    private String guardianName;

    @Column(name = "guardian_phone", nullable = false, length = 40)
    private String guardianPhone;

    @Column(name = "home_address", nullable = false, length = 500)
    private String homeAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected SchoolParticipant() {}

    public SchoolParticipant(School school, DocumentUpload photo, String fullName, String gender, int age,
            String guardianName, String guardianPhone, String homeAddress) {
        this.school = school;
        this.photo = photo;
        this.fullName = fullName;
        this.gender = gender;
        this.age = age;
        this.guardianName = guardianName;
        this.guardianPhone = guardianPhone;
        this.homeAddress = homeAddress;
    }

    public UUID getId() { return id; }
    public School getSchool() { return school; }
    public DocumentUpload getPhoto() { return photo; }
    public String getFullName() { return fullName; }
    public String getGender() { return gender; }
    public int getAge() { return age; }
    public String getGuardianName() { return guardianName; }
    public String getGuardianPhone() { return guardianPhone; }
    public String getHomeAddress() { return homeAddress; }
}
