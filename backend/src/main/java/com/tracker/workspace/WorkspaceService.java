package com.tracker.workspace;

import com.tracker.user.User;
import com.tracker.user.UserRepository;
import com.tracker.workspace.dto.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceService
{
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final WorkspaceMembershipService membershipService;

    @Transactional
    public WorkspaceResponse create(CreateWorkspaceRequest request, UUID currentUserId)
    {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user no longer exists"
                ));

        Workspace workspace = new Workspace();

        workspace.setName(request.Name().trim());

        Workspace savedWorkspace = workspaceRepository.save(workspace);

        WorkspaceMember ownerMembership = new WorkspaceMember();
        ownerMembership.setWorkspace(savedWorkspace);
        ownerMembership.setUser(currentUser);
        ownerMembership.setRole(WorkspaceRole.OWNER);

        workspaceMemberRepository.save(ownerMembership);

        return new WorkspaceResponse(
                savedWorkspace.getId(),
                savedWorkspace.getName()
        );
    }

    public List<WorkspaceResponse> listForUser(UUID currentUserId) {
        return workspaceRepository.findAllForUser(currentUserId).stream()
                .map(workspace -> new WorkspaceResponse(workspace.getId(), workspace.getName()))
                .toList();
    }

    public List<ArchivedWorkspaceResponse> listArchivedForOwner(UUID currentUserId) {
        return workspaceRepository.findArchivedForUserAndRole(
                        currentUserId, WorkspaceRole.OWNER).stream()
                .map(workspace -> new ArchivedWorkspaceResponse(
                        workspace.getId(), workspace.getName(), workspace.getDeletedAt()))
                .toList();
    }

    @Transactional
    public WorkspaceResponse get(UUID workspaceId, UUID currentUserId) {
        Workspace workspace = membershipService.requireMember(workspaceId, currentUserId)
                .getWorkspace();
        return new WorkspaceResponse(workspace.getId(), workspace.getName());
    }

    @Transactional
    public WorkspaceResponse rename(
            UUID workspaceId,
            UUID currentUserId,
            RenameWorkspaceRequest request
    ) {
        Workspace workspace = membershipService
                .requireOwner(workspaceId, currentUserId)
                .getWorkspace();

        workspace.setName(request.name().trim());

        return new WorkspaceResponse(workspace.getId(), workspace.getName());
    }

    @Transactional
    public void archive(UUID workspaceId, UUID currentUserId) {
        Workspace workspace = membershipService.requireOwner(workspaceId, currentUserId)
                .getWorkspace();
        workspace.setDeletedAt(Instant.now());
    }

    @Transactional
    public WorkspaceResponse restore(UUID workspaceId, UUID currentUserId) {
        Workspace workspace = membershipService
                .requireOwnerIncludingArchived(workspaceId, currentUserId)
                .getWorkspace();
        if (workspace.getDeletedAt() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Workspace is not archived");
        }
        workspace.setDeletedAt(null);
        return new WorkspaceResponse(workspace.getId(), workspace.getName());
    }

}

