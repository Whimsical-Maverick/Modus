package com.tracker.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface ActivityLogRepository
        extends JpaRepository<ActivityLog, UUID> {
    List<ActivityLog> findTop50ByBoardIdOrderByCreatedAtDesc(UUID boardId);

    @Modifying
    @Query("delete from ActivityLog log where log.boardId = :boardId")
    void deleteByBoardId(@Param("boardId") UUID boardId);
}
