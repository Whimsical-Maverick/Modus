package com.tracker.workspace;

import com.tracker.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceArchiveServiceTests {
    @Mock WorkspaceRepository workspaceRepository;
    @Mock WorkspaceMemberRepository memberRepository;
    @Mock UserRepository userRepository;
    @Mock WorkspaceMembershipService membershipService;
    @InjectMocks WorkspaceService workspaceService;

    @Test
    void ownerCanArchiveWorkspaceWithoutDeletingIt() {
        UUID workspaceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Workspace workspace = workspace(workspaceId);
        when(membershipService.requireOwner(workspaceId, ownerId))
                .thenReturn(membership(workspace));

        workspaceService.archive(workspaceId, ownerId);

        assertNotNull(workspace.getDeletedAt());
        verify(membershipService).requireOwner(workspaceId, ownerId);
    }

    @Test
    void ownerCanRestoreArchivedWorkspace() {
        UUID workspaceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Workspace workspace = workspace(workspaceId);
        workspace.setDeletedAt(Instant.now());
        when(membershipService.requireOwnerIncludingArchived(workspaceId, ownerId))
                .thenReturn(membership(workspace));

        var response = workspaceService.restore(workspaceId, ownerId);

        assertEquals(workspaceId, response.id());
        assertNull(workspace.getDeletedAt());
    }

    @Test
    void restoringActiveWorkspaceConflicts() {
        UUID workspaceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(membershipService.requireOwnerIncludingArchived(workspaceId, ownerId))
                .thenReturn(membership(workspace(workspaceId)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> workspaceService.restore(workspaceId, ownerId));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void archivedListIncludesOnlyOwnerQueryResults() {
        UUID ownerId = UUID.randomUUID();
        Workspace archived = workspace(UUID.randomUUID());
        archived.setDeletedAt(Instant.now());
        when(workspaceRepository.findArchivedForUserAndRole(ownerId, WorkspaceRole.OWNER))
                .thenReturn(List.of(archived));

        var result = workspaceService.listArchivedForOwner(ownerId);

        assertEquals(1, result.size());
        assertEquals(archived.getDeletedAt(), result.get(0).archivedAt());
    }

    private static Workspace workspace(UUID id) {
        Workspace workspace = new Workspace();
        workspace.setId(id);
        workspace.setName("Modus");
        return workspace;
    }

    private static WorkspaceMember membership(Workspace workspace) {
        WorkspaceMember membership = new WorkspaceMember();
        membership.setWorkspace(workspace);
        membership.setRole(WorkspaceRole.OWNER);
        return membership;
    }
}
