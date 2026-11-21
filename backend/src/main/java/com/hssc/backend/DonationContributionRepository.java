package com.hssc.backend;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface DonationContributionRepository extends JpaRepository<DonationContribution, UUID> {
    Optional<DonationContribution> findByReference(String reference);
}
