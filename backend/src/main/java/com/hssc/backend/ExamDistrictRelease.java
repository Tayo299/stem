package com.hssc.backend;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exam_district_releases", uniqueConstraints =
        @UniqueConstraint(name = "uk_exam_district_release", columnNames = {"exam_id", "district_id"}))
public class ExamDistrictRelease {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "district_id", nullable = false)
    private District district;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "released_by")
    private UserAccount releasedBy;
    @Column(name = "released_at", nullable = false) private Instant releasedAt = Instant.now();

    protected ExamDistrictRelease() {}

    public ExamDistrictRelease(Exam exam, District district, UserAccount releasedBy) {
        this.exam = exam;
        this.district = district;
        this.releasedBy = releasedBy;
    }
}
