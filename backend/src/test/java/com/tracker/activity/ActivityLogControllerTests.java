package com.tracker.activity;

import com.tracker.board.BoardRepository;
import com.tracker.workspace.WorkspaceMembershipService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityLogControllerTests {
    @Mock BoardRepository boardRepository;
    @Mock WorkspaceMembershipService membershipService;
    @Mock ActivityLogRepository activityLogRepository;
    @InjectMocks ActivityLogController controller;

    @Test
    void memberCanReadRecentBoardActivity() {
        UUID boardId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ActivityLog log = new ActivityLog();
        log.setId(UUID.randomUUID());
        log.setCardId(UUID.randomUUID());
        log.setDescription("Moved a card");
        log.setCreatedAt(Instant.now());
        when(boardRepository.findWorkspaceIdByBoardId(boardId))
                .thenReturn(Optional.of(workspaceId));
        when(activityLogRepository.findTop50ByBoardIdOrderByCreatedAtDesc(boardId))
                .thenReturn(List.of(log));

        var result = controller.list(boardId, userId);

        verify(membershipService).requireMember(workspaceId, userId);
        assertEquals(1, result.size());
        assertEquals("Moved a card", result.get(0).description());
    }

    @Test
    void nonmemberCannotReadBoardActivity() {
        UUID boardId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(boardRepository.findWorkspaceIdByBoardId(boardId))
                .thenReturn(Optional.of(workspaceId));
        when(membershipService.requireMember(workspaceId, userId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertThrows(ResponseStatusException.class, () -> controller.list(boardId, userId));
        verify(activityLogRepository, never())
                .findTop50ByBoardIdOrderByCreatedAtDesc(boardId);
    }
}
