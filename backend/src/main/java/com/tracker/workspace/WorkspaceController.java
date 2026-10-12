package com.tracker.workspace;

import com.tracker.workspace.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController
{
    private final WorkspaceService workspaceService;

    @GetMapping
    public List<WorkspaceResponse> list(@AuthenticationPrincipal UUID currentUserId) {
        return workspaceService.listForUser(currentUserId);
    }

    @GetMapping("/{workspaceId}")
    public WorkspaceResponse get(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        return workspaceService.get(workspaceId, currentUserId);
    }

    @PostMapping("/{workspaceId}/members")
    public ResponseEntity<WorkspaceMemberResponse> addMember(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody AddWorkspaceMemberRequest request,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspaceService.addMember(workspaceId, currentUserId, request));
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> create(@Valid @RequestBody CreateWorkspaceRequest request
                                                    , @AuthenticationPrincipal UUID currentUserId)
    {
        WorkspaceResponse response =
                workspaceService.create(request, currentUserId);

        return ResponseEntity
                .created(URI.create("/api/v1/workspaces/" + response.id()))
                .body(response);
    }

    @PatchMapping
    public WorkspaceResponse rename(
            @PathVariable UUID WorkspaceId,
            @Valid @RequestBody RenameWorkspaceRequest request,
            @AuthenticationPrincipal UUID currentUserId)
    {
        return workspaceService.rename(WorkspaceId,currentUserId,request);
    }
}

