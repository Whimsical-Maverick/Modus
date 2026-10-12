package com.tracker.workspace;

import com.tracker.user.User;
import com.tracker.user.UserRepository;
import com.tracker.workspace.dto.CreateWorkspaceInvitationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceInvitationServiceTests {
    @Mock WorkspaceInvitationRepository invitationRepository;
    @Mock WorkspaceMemberRepository memberRepository;
    @Mock WorkspaceMembershipService membershipService;
    @Mock UserRepository userRepository;
    @InjectMocks WorkspaceInvitationService invitationService;

    @Test
    void creatingInvitationDoesNotCreateMembership() {
        UUID workspaceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        Workspace workspace = new Workspace();
        workspace.setId(workspaceId);
        workspace.setName("Modus");
        User owner = user(ownerId, "owner@example.com");
        User invitee = user(inviteeId, "invitee@example.com");
        WorkspaceMember ownerMembership = new WorkspaceMember();
        ownerMembership.setWorkspace(workspace);

        when(membershipService.requireOwner(workspaceId, ownerId)).thenReturn(ownerMembership);
        when(userRepository.findByEmail(invitee.getEmail())).thenReturn(Optional.of(invitee));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(invitationRepository.saveAndFlush(any(WorkspaceInvitation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = invitationService.create(workspaceId, ownerId,
                new CreateWorkspaceInvitationRequest("  INVITEE@example.com  "));

        assertEquals(InvitationStatus.PENDING, response.status());
        verify(memberRepository, never()).saveAndFlush(any(WorkspaceMember.class));
    }

    @Test
    void acceptingInvitationCreatesMemberAndMarksInvitationAccepted() {
        UUID invitationId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        User invitee = user(inviteeId, "invitee@example.com");
        WorkspaceInvitation invitation = pendingInvitation(workspace, invitee);
        when(invitationRepository.findByIdForUpdate(invitationId))
                .thenReturn(Optional.of(invitation));

        invitationService.accept(invitationId, inviteeId);

        assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());
        verify(memberRepository).saveAndFlush(any(WorkspaceMember.class));
    }

    @Test
    void differentUserCannotAcceptInvitation() {
        UUID invitationId = UUID.randomUUID();
        WorkspaceInvitation invitation = pendingInvitation(new Workspace(),
                user(UUID.randomUUID(), "invitee@example.com"));
        when(invitationRepository.findByIdForUpdate(invitationId))
                .thenReturn(Optional.of(invitation));

        assertThrows(ResponseStatusException.class,
                () -> invitationService.accept(invitationId, UUID.randomUUID()));

        verify(memberRepository, never()).saveAndFlush(any(WorkspaceMember.class));
    }

    @Test
    void expiredInvitationCannotBeAccepted() {
        UUID invitationId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        WorkspaceInvitation invitation = pendingInvitation(new Workspace(),
                user(inviteeId, "invitee@example.com"));
        invitation.setExpiresAt(Instant.now().minusSeconds(1));
        when(invitationRepository.findByIdForUpdate(invitationId))
                .thenReturn(Optional.of(invitation));

        assertThrows(ResponseStatusException.class,
                () -> invitationService.accept(invitationId, inviteeId));

        verify(memberRepository, never()).saveAndFlush(any(WorkspaceMember.class));
    }

    private static User user(UUID id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

    private static WorkspaceInvitation pendingInvitation(Workspace workspace, User invitedUser) {
        WorkspaceInvitation invitation = new WorkspaceInvitation();
        invitation.setWorkspace(workspace);
        invitation.setInvitedUser(invitedUser);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(Instant.now().plusSeconds(3600));
        return invitation;
    }
}
