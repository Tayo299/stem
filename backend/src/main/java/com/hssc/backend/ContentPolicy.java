package com.hssc.backend;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "content_policies")
public class ContentPolicy {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true, length = 100) private String slug;
    @Column(nullable = false, length = 180) private String title;
    @Column(nullable = false, columnDefinition = "text") private String content;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    protected ContentPolicy() {}

    public ContentPolicy(String slug, String title, String content, boolean active, int displayOrder) {
        this.slug = slug;
        this.title = title;
        this.content = content;
        this.active = active;
        this.displayOrder = displayOrder;
    }

    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isActive() { return active; }
    public int getDisplayOrder() { return displayOrder; }
    public void update(String slug, String title, String content, boolean active, int displayOrder) {
        this.slug = slug;
        this.title = title;
        this.content = content;
        this.active = active;
        this.displayOrder = displayOrder;
    }
}
