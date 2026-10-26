package com.hssc.backend;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface AuditRepository extends JpaRepository<AuditEvent,UUID> {
    java.util.List<AuditEvent> findTop200ByOrderByCreatedAtDesc();
}
