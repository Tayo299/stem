package com.hssc.backend;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SchoolParticipantRepository extends JpaRepository<SchoolParticipant, UUID> {
    List<SchoolParticipant> findBySchoolIdOrderByFullNameAsc(UUID schoolId);
}
