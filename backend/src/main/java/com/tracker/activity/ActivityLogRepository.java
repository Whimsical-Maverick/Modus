package com.tracker.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ActivityLogRepository
        extends JpaRepository<ActivityLog, UUID> {
    List<ActivityLog> findTop50ByBoardIdOrderByCreatedAtDesc(UUID boardId);
}
