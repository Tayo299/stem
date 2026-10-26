package com.hssc.backend;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ZoneRepository extends JpaRepository<Zone,UUID> { List<Zone> findByDistrictId(UUID id); }
