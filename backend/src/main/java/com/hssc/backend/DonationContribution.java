package com.hssc.backend;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "donation_contributions")
public class DonationContribution {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String reference;

    @Column(name = "amount_ngn", nullable = false)
    private long amountNgn;

    @Column(name = "donor_name", nullable = false, length = 180)
    private String donorName;

    @Column(nullable = false, length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SponsorPaymentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    protected DonationContribution() {}

    public DonationContribution(String reference, long amountNgn, String donorName, String email) {
        this.reference = reference;
        this.amountNgn = amountNgn;
        this.donorName = donorName;
        this.email = email;
        this.status = SponsorPaymentStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public String getReference() { return reference; }
    public long getAmountNgn() { return amountNgn; }
    public String getEmail() { return email; }
    public SponsorPaymentStatus getStatus() { return status; }

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
