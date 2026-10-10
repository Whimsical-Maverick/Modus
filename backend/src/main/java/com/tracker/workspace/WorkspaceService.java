package com.tracker.workspace;

import com.tracker.user.User;
import com.tracker.user.UserRepository;
import com.tracker.workspace.dto.CreateWorkspaceRequest;
import com.tracker.workspace.dto.WorkspaceResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceService
{
    private final WorkspaceRepository WorkspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

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

        Workspace savedWorkspace = WorkspaceRepository.save(workspace);

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

}

