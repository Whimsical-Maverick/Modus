package com.tracker.workspace;

import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    @Query("select membership.workspace from WorkspaceMember membership " +
            "where membership.user.id = :userId and membership.workspace.deletedAt is null " +
            "order by membership.workspace.name")
    List<Workspace> findAllForUser(@Param("userId") UUID userId);

    @Query("select membership.workspace from WorkspaceMember membership " +
            "where membership.user.id = :userId and membership.role = :role " +
            "and membership.workspace.deletedAt is not null " +
            "order by membership.workspace.deletedAt desc")
    List<Workspace> findArchivedForUserAndRole(
            @Param("userId") UUID userId, @Param("role") WorkspaceRole role);
}

