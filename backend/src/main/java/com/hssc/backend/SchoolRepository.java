package com.hssc.backend;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SchoolRepository extends JpaRepository<School,UUID> {
    Optional<School> findByAccountId(UUID accountId);
    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);
    boolean existsByNameIgnoreCaseAndZone_Id(String name,UUID zoneId);
    List<School> findByZoneId(UUID zoneId);
    long countByZoneId(UUID zoneId);
    long countByAccountStatus(AccountStatus status);
}
