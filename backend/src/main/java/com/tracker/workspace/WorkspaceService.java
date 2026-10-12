package com.tracker.workspace;

import com.tracker.user.User;
import com.tracker.user.UserRepository;
import com.tracker.workspace.dto.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
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

    @Transactional
    public WorkspaceResponse get(UUID workspaceId, UUID currentUserId) {
        Workspace workspace = membershipService.requireMember(workspaceId, currentUserId)
                .getWorkspace();
        return new WorkspaceResponse(workspace.getId(), workspace.getName());
    }

    @Transactional
    public WorkspaceMemberResponse addMember(
            UUID workspaceId,
            UUID currentUserId,
            AddWorkspaceMemberRequest request
    ) {
        Workspace workspace = membershipService.requireOwner(workspaceId, currentUserId)
                .getWorkspace();

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User newMember = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        if (workspaceMemberRepository.existsByWorkspace_IdAndUser_Id(
                workspaceId, newMember.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "User is already a workspace member");
        }

        WorkspaceMember membership = new WorkspaceMember();
        membership.setWorkspace(workspace);
        membership.setUser(newMember);
        membership.setRole(WorkspaceRole.MEMBER);

        try {
            workspaceMemberRepository.saveAndFlush(membership);
        } catch (DataIntegrityViolationException ex) {
            // The unique constraint handles two invitations racing each other.
            if (ex.getMostSpecificCause() instanceof SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "User is already a workspace member", ex);
            }
            throw ex;
        }

        return new WorkspaceMemberResponse(
                newMember.getId(), newMember.getEmail(), membership.getRole());
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

}

