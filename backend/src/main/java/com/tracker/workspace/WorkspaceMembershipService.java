package com.tracker.workspace;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceMembershipService {

    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceMember requireMember(UUID workspaceId, UUID userId) {
        return workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Workspace not found"));
    }

    public WorkspaceMember requireOwner(UUID workspaceId, UUID userId) {
        WorkspaceMember membership = requireMember(workspaceId, userId);
        if (membership.getRole() != WorkspaceRole.OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only workspace owners can add members");
        }
        return membership;
    }
}
