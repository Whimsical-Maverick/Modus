package com.tracker.workspace;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, UUID> {
    Optional<WorkspaceInvitation> findByWorkspace_IdAndInvitedUser_IdAndStatus(
            UUID workspaceId, UUID invitedUserId, InvitationStatus status);

    @Query("select i from WorkspaceInvitation i join fetch i.workspace " +
            "join fetch i.invitedUser where i.invitedUser.id = :userId " +
            "and i.status = :status and i.workspace.deletedAt is null " +
            "order by i.createdAt desc")
    List<WorkspaceInvitation> findAllForUser(
            @Param("userId") UUID userId, @Param("status") InvitationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from WorkspaceInvitation i where i.id = :id")
    Optional<WorkspaceInvitation> findByIdForUpdate(@Param("id") UUID id);
}
