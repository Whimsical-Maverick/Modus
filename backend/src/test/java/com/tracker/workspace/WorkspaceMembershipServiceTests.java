package com.tracker.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceMembershipServiceTests {
    @Mock WorkspaceMemberRepository memberRepository;
    @InjectMocks WorkspaceMembershipService membershipService;

    @Test
    void archivedWorkspaceIsHiddenFromNormalMembers() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Workspace workspace = new Workspace();
        workspace.setDeletedAt(Instant.now());
        WorkspaceMember membership = membership(workspace, WorkspaceRole.MEMBER);
        when(memberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId))
                .thenReturn(Optional.of(membership));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> membershipService.requireMember(workspaceId, userId));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void archivedOwnerCanStillRestore() {
        UUID workspaceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Workspace workspace = new Workspace();
        workspace.setDeletedAt(Instant.now());
        WorkspaceMember membership = membership(workspace, WorkspaceRole.OWNER);
        when(memberRepository.findByWorkspace_IdAndUser_Id(workspaceId, ownerId))
                .thenReturn(Optional.of(membership));

        assertSame(membership,
                membershipService.requireOwnerIncludingArchived(workspaceId, ownerId));
    }

    @Test
    void archivedNonOwnerCannotRestore() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Workspace workspace = new Workspace();
        workspace.setDeletedAt(Instant.now());
        when(memberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId))
                .thenReturn(Optional.of(membership(workspace, WorkspaceRole.MEMBER)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> membershipService.requireOwnerIncludingArchived(workspaceId, userId));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    private static WorkspaceMember membership(Workspace workspace, WorkspaceRole role) {
        WorkspaceMember membership = new WorkspaceMember();
        membership.setWorkspace(workspace);
        membership.setRole(role);
        return membership;
    }
}
