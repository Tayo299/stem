package com.hssc.backend;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="user_accounts", uniqueConstraints=@UniqueConstraint(name="uk_account_email", columnNames="email"))
public class UserAccount {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private java.util.UUID id;
    @Column(nullable=false, length=254) private String email;
    @Column(nullable=false) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private Role role;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private AccountStatus status;
    @Column(nullable=false, updatable=false) private Instant createdAt;
    protected UserAccount() {}
    public UserAccount(String email, String passwordHash, Role role, AccountStatus status) {
        this.email=email.toLowerCase(java.util.Locale.ROOT); this.passwordHash=passwordHash; this.role=role; this.status=status;
        this.createdAt=Instant.now();
    }
    public java.util.UUID getId(){return id;} public String getEmail(){return email;} public String getPasswordHash(){return passwordHash;}
    public Role getRole(){return role;} public AccountStatus getStatus(){return status;} public Instant getCreatedAt(){return createdAt;}
    public void setStatus(AccountStatus status){this.status=status;}
}
