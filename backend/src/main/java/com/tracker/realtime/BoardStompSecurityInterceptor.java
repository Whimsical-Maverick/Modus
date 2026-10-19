package com.tracker.realtime;

import com.tracker.auth.JwtService;
import com.tracker.board.BoardRepository;
import com.tracker.workspace.WorkspaceMembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BoardStompSecurityInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final BoardRepository boardRepository;
    private final WorkspaceMembershipService membershipService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (accessor.getCommand() == StompCommand.CONNECT) {
            String header = accessor.getFirstNativeHeader("Authorization");

            if (header == null || !header.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Missing access token");
            }

            UUID userId = jwtService.extractUserId(
                    header.substring("Bearer ".length()));

            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    AuthorityUtils.NO_AUTHORITIES
            ));
        }

        if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            Principal principal = accessor.getUser();
            if (!(principal instanceof UsernamePasswordAuthenticationToken auth)
                    || !(auth.getPrincipal() instanceof UUID userId)) {
                throw new IllegalArgumentException("Unauthenticated subscription");
            }

            String destination = accessor.getDestination();
            String prefix = "/topic/board/";

            if (destination == null
                    || !destination.startsWith(prefix)) {
                throw new IllegalArgumentException(
                        "Subscription destination not allowed");
            }

            UUID boardId;
            try {
                boardId = UUID.fromString(
                        destination.substring(prefix.length()));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "Invalid board destination", ex);
            }

            UUID workspaceId = boardRepository
                    .findWorkspaceIdByBoardId(boardId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Board not found"));

            membershipService.requireMember(workspaceId, userId);
        }

        // Only the server publishes board events. Clients cannot SEND to
        // /topic or /app destinations.
        if (accessor.getCommand() == StompCommand.SEND) {
            throw new IllegalArgumentException(
                    "Client messages are not allowed");
        }

        return message;
    }
}