package com.tracker.board;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BoardListRepository extends JpaRepository<BoardList, UUID>
{
    boolean existsByBoard_IdAndSortOrder(UUID boardId, Integer sortOrder);
}
