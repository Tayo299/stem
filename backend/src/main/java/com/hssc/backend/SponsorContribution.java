package com.hssc.backend;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sponsor_contributions")
public class SponsorContribution {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String reference;

    @Column(name = "package_code", nullable = false, length = 20)
    private String packageCode;

    @Column(name = "package_name", nullable = false, length = 80)
    private String packageName;

    @Column(name = "amount_ngn", nullable = false)
    private long amountNgn;

    @Column(name = "organization_name", nullable = false, length = 180)
    private String organizationName;

    @Column(name = "contact_name", nullable = false, length = 180)
    private String contactName;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 40)
    private String phone;

    @Column(length = 255)
    private String website;

    @Column(length = 1200)
    private String message;

    @Column(name = "privacy_consent", nullable = false)
    private boolean privacyConsent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SponsorPaymentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    protected SponsorContribution() {}

    public SponsorContribution(String reference, SponsorPackage plan, String organizationName,
            String contactName, String email, String phone, String website, String message, boolean privacyConsent) {
        this.reference = reference;
        this.packageCode = plan.name();
        this.packageName = plan.displayName();
        this.amountNgn = plan.amountNaira();
        this.organizationName = organizationName;
        this.contactName = contactName;
        this.email = email;
        this.phone = phone;
        this.website = website;
        this.message = message;
        this.privacyConsent = privacyConsent;
        this.status = SponsorPaymentStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getReference() { return reference; }
    public String getPackageCode() { return packageCode; }
    public String getPackageName() { return packageName; }
    public long getAmountNgn() { return amountNgn; }
    public String getOrganizationName() { return organizationName; }
    public String getContactName() { return contactName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getWebsite() { return website; }
    public String getMessage() { return message; }
    public boolean isPrivacyConsent() { return privacyConsent; }
    public SponsorPaymentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPaidAt() { return paidAt; }

    public void markPaid() {
        if (status != SponsorPaymentStatus.PAID) {
            status = SponsorPaymentStatus.PAID;
            paidAt = Instant.now();
        }
    }

    public void markFailed() {
        if (status == SponsorPaymentStatus.PENDING) status = SponsorPaymentStatus.FAILED;
    }
}
