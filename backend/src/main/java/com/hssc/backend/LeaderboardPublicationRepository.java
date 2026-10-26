package com.hssc.backend;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LeaderboardPublicationRepository extends JpaRepository<LeaderboardPublication,UUID> {}
