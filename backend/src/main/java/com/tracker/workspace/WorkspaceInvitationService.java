package com.tracker.workspace;

import com.tracker.user.User;
import com.tracker.user.UserRepository;
import com.tracker.workspace.dto.CreateWorkspaceInvitationRequest;
import com.tracker.workspace.dto.WorkspaceInvitationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceInvitationService {
    private final WorkspaceInvitationRepository invitationRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final WorkspaceMembershipService membershipService;
    private final UserRepository userRepository;

    @Transactional
    public WorkspaceInvitationResponse create(
            UUID workspaceId, UUID currentUserId, CreateWorkspaceInvitationRequest request) {
        Workspace workspace = membershipService.requireOwner(workspaceId, currentUserId)
                .getWorkspace();
        User invitedUser = userRepository.findByEmail(
                        request.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        if (memberRepository.existsByWorkspace_IdAndUser_Id(workspaceId, invitedUser.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User is already a workspace member");
        }

        Instant now = Instant.now();
        invitationRepository.findByWorkspace_IdAndInvitedUser_IdAndStatus(
                workspaceId, invitedUser.getId(), InvitationStatus.PENDING
        ).ifPresent(existing -> {
            if (existing.getExpiresAt().isAfter(now)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "User already has a pending invitation");
            }
            existing.setStatus(InvitationStatus.REVOKED);
            invitationRepository.flush();
        });

        User inviter = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user no longer exists"));

        WorkspaceInvitation invitation = new WorkspaceInvitation();
        invitation.setWorkspace(workspace);
        invitation.setInvitedUser(invitedUser);
        invitation.setInvitedByUser(inviter);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setCreatedAt(now);
        invitation.setExpiresAt(now.plus(7, ChronoUnit.DAYS));

        try {
            return toResponse(invitationRepository.saveAndFlush(invitation));
        } catch (DataIntegrityViolationException ex) {
            // The unique pending-invitation index also protects concurrent requests.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User already has a pending invitation", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationResponse> listForUser(UUID currentUserId) {
        Instant now = Instant.now();
        return invitationRepository.findAllForUser(currentUserId, InvitationStatus.PENDING)
                .stream()
                .filter(invitation -> invitation.getExpiresAt().isAfter(now))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationResponse> listForWorkspace(
            UUID workspaceId, UUID currentUserId) {
        membershipService.requireOwner(workspaceId, currentUserId);
        Instant now = Instant.now();
        return invitationRepository.findAllForWorkspace(
                        workspaceId, InvitationStatus.PENDING).stream()
                .filter(invitation -> invitation.getExpiresAt().isAfter(now))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void accept(UUID invitationId, UUID currentUserId) {
        WorkspaceInvitation invitation = requirePendingInvitationForUser(
                invitationId, currentUserId);

        if (memberRepository.existsByWorkspace_IdAndUser_Id(
                invitation.getWorkspace().getId(), currentUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User is already a workspace member");
        }

        WorkspaceMember membership = new WorkspaceMember();
        membership.setWorkspace(invitation.getWorkspace());
        membership.setUser(invitation.getInvitedUser());
        membership.setRole(WorkspaceRole.MEMBER);

        try {
            memberRepository.saveAndFlush(membership);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User is already a workspace member", ex);
        }
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
    }

    @Transactional
    public void decline(UUID invitationId, UUID currentUserId) {
        WorkspaceInvitation invitation = requirePendingInvitationForUser(
                invitationId, currentUserId);
        invitation.setStatus(InvitationStatus.DECLINED);
    }

    @Transactional
    public void revoke(UUID workspaceId, UUID invitationId, UUID currentUserId) {
        membershipService.requireOwner(workspaceId, currentUserId);
        WorkspaceInvitation invitation = invitationRepository.findByIdForUpdate(invitationId)
                .filter(found -> found.getWorkspace().getId().equals(workspaceId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invitation not found"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invitation is no longer pending");
        }
        invitation.setStatus(InvitationStatus.REVOKED);
    }

    private WorkspaceInvitation requirePendingInvitationForUser(
            UUID invitationId, UUID currentUserId) {
        WorkspaceInvitation invitation = invitationRepository.findByIdForUpdate(invitationId)
                .filter(found -> found.getInvitedUser().getId().equals(currentUserId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invitation not found"));

        if (invitation.getWorkspace().getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Invitation not found");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING
                || !invitation.getExpiresAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invitation is no longer available");
        }
        return invitation;
    }

    private WorkspaceInvitationResponse toResponse(WorkspaceInvitation invitation) {
        return new WorkspaceInvitationResponse(
                invitation.getId(),
                invitation.getWorkspace().getId(),
                invitation.getWorkspace().getName(),
                invitation.getInvitedUser().getId(),
                invitation.getInvitedUser().getEmail(),
                invitation.getStatus(),
                invitation.getCreatedAt(),
                invitation.getExpiresAt());
    }
}
