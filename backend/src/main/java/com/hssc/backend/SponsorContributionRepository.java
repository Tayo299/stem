package com.hssc.backend;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SponsorContributionRepository extends JpaRepository<SponsorContribution, UUID> {
    Optional<SponsorContribution> findByReference(String reference);
    List<SponsorContribution> findAllByOrderByCreatedAtDesc();
}
