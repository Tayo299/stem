package com.hssc.backend;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface DistrictRepository extends JpaRepository<District,UUID> { Optional<District> findByCode(String code); }
