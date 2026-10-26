package com.hssc.backend;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ContentPolicyRepository extends JpaRepository<ContentPolicy, UUID> {
    List<ContentPolicy> findByActiveTrueOrderByDisplayOrderAscTitleAsc();
    List<ContentPolicy> findAllByOrderByDisplayOrderAscTitleAsc();
}
