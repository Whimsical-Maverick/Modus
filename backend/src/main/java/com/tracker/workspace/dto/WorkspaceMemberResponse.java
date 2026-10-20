package com.tracker.workspace.dto;

import com.tracker.workspace.WorkspaceRole;

import java.util.UUID;

public record WorkspaceMemberResponse(
        UUID userId, String username, String email, WorkspaceRole role) {}
