package com.tracker.workspace;

import com.tracker.workspace.dto.CreateWorkspaceRequest;
import com.tracker.workspace.dto.WorkspaceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController
{
    private final WorkspaceService workspaceService;
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
}

