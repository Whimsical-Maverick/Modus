package com.tracker.board;

import com.tracker.activity.ActivityLogRepository;
import com.tracker.card.CardRepository;
import com.tracker.realtime.events.BoardContentDeletedEvent;
import com.tracker.workspace.Workspace;
import com.tracker.workspace.WorkspaceMembershipService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardServiceDeleteTests {
    @Mock BoardRepository boardRepository;
    @Mock BoardListRepository boardListRepository;
    @Mock CardRepository cardRepository;
    @Mock ActivityLogRepository activityLogRepository;
    @Mock WorkspaceMembershipService membershipService;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks BoardService service;

    @Test
    void boardDeleteRemovesActivityAndPublishesAfterFlush() {
        UUID userId = UUID.randomUUID();
        Board board = board();
        when(boardRepository.findById(board.getId())).thenReturn(Optional.of(board));

        service.deleteBoard(board.getId(), userId);

        var order = inOrder(membershipService, activityLogRepository, boardRepository, eventPublisher);
        order.verify(membershipService).requireOwner(board.getWorkspace().getId(), userId);
        order.verify(activityLogRepository).deleteByBoardId(board.getId());
        order.verify(boardRepository).delete(board);
        order.verify(boardRepository).flush();
        order.verify(eventPublisher).publishEvent(
                new BoardContentDeletedEvent(board.getId(), "BOARD_DELETED", board.getId()));
    }

    @Test
    void nonOwnerCannotDeleteBoardOrItsChildren() {
        UUID userId = UUID.randomUUID();
        Board board = board();
        when(boardRepository.findById(board.getId())).thenReturn(Optional.of(board));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(membershipService).requireOwner(board.getWorkspace().getId(), userId);

        var error = assertThrows(ResponseStatusException.class,
                () -> service.deleteBoard(board.getId(), userId));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(activityLogRepository, eventPublisher);
        org.mockito.Mockito.verify(boardRepository, org.mockito.Mockito.never()).delete(any());
    }

    @Test
    void listDeleteRequiresOwnerAndPublishesListId() {
        UUID userId = UUID.randomUUID();
        Board board = board();
        BoardList list = new BoardList();
        list.setId(UUID.randomUUID());
        list.setBoard(board);
        when(boardListRepository.findById(list.getId())).thenReturn(Optional.of(list));

        service.deleteList(list.getId(), userId);

        var order = inOrder(membershipService, boardListRepository, eventPublisher);
        order.verify(membershipService).requireOwner(board.getWorkspace().getId(), userId);
        order.verify(boardListRepository).delete(list);
        order.verify(boardListRepository).flush();
        order.verify(eventPublisher).publishEvent(
                new BoardContentDeletedEvent(board.getId(), "LIST_DELETED", list.getId()));
    }

    private static Board board() {
        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        Board board = new Board();
        board.setId(UUID.randomUUID());
        board.setWorkspace(workspace);
        return board;
    }
}
