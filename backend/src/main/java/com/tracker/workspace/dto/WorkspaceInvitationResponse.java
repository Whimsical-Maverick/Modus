package com.tracker.workspace.dto;

import com.tracker.workspace.InvitationStatus;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceInvitationResponse(
        UUID id,
        UUID workspaceId,
        String workspaceName,
        UUID invitedUserId,
        String invitedEmail,
        InvitationStatus status,
        Instant createdAt,
        Instant expiresAt
) {}
