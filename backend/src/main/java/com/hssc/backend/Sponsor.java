package com.hssc.backend;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sponsors")
public class Sponsor {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 160) private String name;
    @Column(length = 180) private String organization;
    @Column(length = 1000) private String description;
    @Column(name = "website_url", length = 500) private String websiteUrl;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    protected Sponsor() {}

    public Sponsor(String name, String organization, String description, String websiteUrl, int displayOrder, boolean active) {
        this.name = name;
        this.organization = organization;
        this.description = description;
        this.websiteUrl = websiteUrl;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getOrganization() { return organization; }
    public String getDescription() { return description; }
    public String getWebsiteUrl() { return websiteUrl; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return active; }
    public void update(String name, String organization, String description, String websiteUrl, int displayOrder, boolean active) {
        this.name = name;
        this.organization = organization;
        this.description = description;
        this.websiteUrl = websiteUrl;
        this.displayOrder = displayOrder;
        this.active = active;
    }
}
