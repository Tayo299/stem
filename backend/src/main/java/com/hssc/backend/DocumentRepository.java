package com.hssc.backend;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface DocumentRepository extends JpaRepository<DocumentUpload,UUID> {
    java.util.List<DocumentUpload> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
}
