package com.tracker.workspace;

import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    @Query("select membership.workspace from WorkspaceMember membership " +
            "where membership.user.id = :userId order by membership.workspace.name")
    List<Workspace> findAllForUser(@Param("userId") UUID userId);
}

