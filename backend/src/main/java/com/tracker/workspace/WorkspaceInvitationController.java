package com.tracker.workspace;

import com.tracker.workspace.dto.CreateWorkspaceInvitationRequest;
import com.tracker.workspace.dto.WorkspaceInvitationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class WorkspaceInvitationController {
    private final WorkspaceInvitationService invitationService;

    @PostMapping("/workspaces/{workspaceId}/invitations")
    public ResponseEntity<WorkspaceInvitationResponse> create(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateWorkspaceInvitationRequest request,
            @AuthenticationPrincipal UUID currentUserId) {
        WorkspaceInvitationResponse response = invitationService.create(
                workspaceId, currentUserId, request);
        return ResponseEntity.created(URI.create(
                "/api/v1/workspace-invitations/" + response.id())).body(response);
    }

    @GetMapping("/workspace-invitations")
    public List<WorkspaceInvitationResponse> list(
            @AuthenticationPrincipal UUID currentUserId) {
        return invitationService.listForUser(currentUserId);
    }

    @GetMapping("/workspaces/{workspaceId}/invitations")
    public List<WorkspaceInvitationResponse> listForWorkspace(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal UUID currentUserId) {
        return invitationService.listForWorkspace(workspaceId, currentUserId);
    }

    @PostMapping("/workspace-invitations/{invitationId}/accept")
    public ResponseEntity<Void> accept(
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal UUID currentUserId) {
        invitationService.accept(invitationId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/workspace-invitations/{invitationId}/decline")
    public ResponseEntity<Void> decline(
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal UUID currentUserId) {
        invitationService.decline(invitationId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/workspaces/{workspaceId}/invitations/{invitationId}")
    public ResponseEntity<Void> revoke(
            @PathVariable UUID workspaceId,
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal UUID currentUserId) {
        invitationService.revoke(workspaceId, invitationId, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
