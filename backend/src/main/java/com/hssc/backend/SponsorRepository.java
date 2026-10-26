package com.hssc.backend;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SponsorRepository extends JpaRepository<Sponsor, UUID> {
    List<Sponsor> findByActiveTrueOrderByDisplayOrderAscNameAsc();
    List<Sponsor> findAllByOrderByDisplayOrderAscNameAsc();
}
