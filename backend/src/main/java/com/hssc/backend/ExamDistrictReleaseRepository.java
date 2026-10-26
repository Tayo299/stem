package com.hssc.backend;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ExamDistrictReleaseRepository extends JpaRepository<ExamDistrictRelease, UUID> {
    boolean existsByExamIdAndDistrictId(UUID examId, UUID districtId);
}
