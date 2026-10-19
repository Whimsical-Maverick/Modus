package com.tracker.board;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardRepository extends JpaRepository<Board, UUID>
{
    @Query("select b.workspace.id from Board b where b.id = :boardid")
    Optional<UUID> findWorkspaceIdByBoardId(@Param("boardId") UUID boardId);
}

