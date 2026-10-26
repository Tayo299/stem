package com.hssc.backend;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface NotificationRepository extends JpaRepository<Notification,UUID> { List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId); }
