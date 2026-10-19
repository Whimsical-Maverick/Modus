package com.tracker.workspace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;
import java.util.List;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID>
{
    @Query("select membership from WorkspaceMember membership " +
            "join fetch membership.workspace " +
            "where membership.workspace.id = :workspaceId " +
            "and membership.user.id = :userId")
    Optional<WorkspaceMember> findByWorkspace_IdAndUser_Id(
            @Param("workspaceId") UUID workspaceId, @Param("userId") UUID userId);

    boolean existsByWorkspace_IdAndUser_Id(UUID workspaceId, UUID userId);

    @Query("select membership from WorkspaceMember membership " +
            "join fetch membership.user where membership.workspace.id = :workspaceId " +
            "order by membership.user.username")
    List<WorkspaceMember> findMembersForWorkspace(@Param("workspaceId") UUID workspaceId);
}
