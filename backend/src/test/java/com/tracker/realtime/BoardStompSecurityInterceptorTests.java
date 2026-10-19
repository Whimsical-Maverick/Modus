package com.tracker.realtime;

import com.tracker.auth.JwtService;
import com.tracker.board.BoardRepository;
import com.tracker.workspace.WorkspaceMembershipService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class BoardStompSecurityInterceptorTests {
    @Mock JwtService jwtService;
    @Mock BoardRepository boardRepository;
    @Mock WorkspaceMembershipService membershipService;
    @Mock MessageChannel channel;
    @InjectMocks BoardStompSecurityInterceptor interceptor;

    @Test
    void connectRequiresBearerToken() {
        Message<?> message = message(StompCommand.CONNECT, null, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> interceptor.preSend(message, channel));
        verifyNoInteractions(jwtService, boardRepository, membershipService);
    }

    @Test
    void connectSetsAuthenticatedUserFromJwt() {
        UUID userId = UUID.randomUUID();
        when(jwtService.extractUserId("valid-token")).thenReturn(userId);
        Message<?> message = message(StompCommand.CONNECT, null,
                "Bearer valid-token", null);

        assertSame(message, interceptor.preSend(message, channel));
        StompHeaderAccessor accessor =
                StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        assertNotNull(accessor);
        UsernamePasswordAuthenticationToken user = assertInstanceOf(
                UsernamePasswordAuthenticationToken.class, accessor.getUser());
        assertSame(userId, user.getPrincipal());
    }

    @Test
    void subscriptionRequiresAuthenticatedUser() {
        Message<?> message = message(StompCommand.SUBSCRIBE,
                "/topic/board/" + UUID.randomUUID(), null, null);

        assertThrows(IllegalArgumentException.class,
                () -> interceptor.preSend(message, channel));
        verifyNoInteractions(boardRepository, membershipService);
    }

    @Test
    void memberMaySubscribeToOwnBoard() {
        UUID userId = UUID.randomUUID();
        UUID boardId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        when(boardRepository.findWorkspaceIdByBoardId(boardId))
                .thenReturn(Optional.of(workspaceId));
        Message<?> message = message(StompCommand.SUBSCRIBE,
                "/topic/board/" + boardId, null, userId);

        assertSame(message, interceptor.preSend(message, channel));
        verify(membershipService).requireMember(workspaceId, userId);
    }

    @Test
    void nonMemberCannotSubscribe() {
        UUID userId = UUID.randomUUID();
        UUID boardId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        when(boardRepository.findWorkspaceIdByBoardId(boardId))
                .thenReturn(Optional.of(workspaceId));
        when(membershipService.requireMember(workspaceId, userId))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Workspace not found"));
        Message<?> message = message(StompCommand.SUBSCRIBE,
                "/topic/board/" + boardId, null, userId);

        assertThrows(ResponseStatusException.class,
                () -> interceptor.preSend(message, channel));
    }

    @Test
    void otherTopicsAndClientSendsAreRejected() {
        UUID userId = UUID.randomUUID();
        Message<?> otherTopic = message(StompCommand.SUBSCRIBE,
                "/topic/board/" + UUID.randomUUID() + "/other", null, userId);
        Message<?> send = message(StompCommand.SEND,
                "/topic/board/" + UUID.randomUUID(), null, userId);

        assertThrows(IllegalArgumentException.class,
                () -> interceptor.preSend(otherTopic, channel));
        assertThrows(IllegalArgumentException.class,
                () -> interceptor.preSend(send, channel));
        verifyNoInteractions(boardRepository, membershipService);
    }

    private static Message<?> message(StompCommand command, String destination,
                                      String authorization, UUID userId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        if (userId != null) {
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    userId, null, AuthorityUtils.NO_AUTHORITIES));
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
