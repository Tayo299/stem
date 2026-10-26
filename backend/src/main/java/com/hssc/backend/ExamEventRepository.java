package com.hssc.backend;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ExamEventRepository extends JpaRepository<ExamEvent,UUID> {}
