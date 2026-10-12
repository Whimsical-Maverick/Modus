package com.tracker.workspace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateWorkspaceInvitationRequest(
        @NotBlank @Email String email
) {}
