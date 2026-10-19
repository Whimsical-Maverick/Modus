package com.tracker.card;

import com.tracker.board.Board;
import com.tracker.board.BoardList;
import com.tracker.board.BoardListRepository;
import com.tracker.card.dto.MoveCardRequest;
import com.tracker.realtime.events.CardMovedEvent;
import com.tracker.user.User;
import com.tracker.user.UserRepository;
import com.tracker.workspace.Workspace;
import com.tracker.workspace.WorkspaceMembershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTests {
    @Mock BoardListRepository boardListRepository;
    @Mock CardRepository cardRepository;
    @Mock WorkspaceMembershipService membershipService;
    @Mock UserRepository userRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks CardService cardService;

    private UUID userId;
    private UUID workspaceId;
    private UUID boardId;
    private UUID cardId;
    private BoardList oldList;
    private BoardList newList;
    private Card card;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        boardId = UUID.randomUUID();
        cardId = UUID.randomUUID();

        Workspace workspace = new Workspace();
        workspace.setId(workspaceId);
        Board board = new Board();
        board.setId(boardId);
        board.setWorkspace(workspace);

        oldList = list(UUID.randomUUID(), board, "To Do");
        newList = list(UUID.randomUUID(), board, "In Progress");
        card = new Card();
        card.setId(cardId);
        card.setList(oldList);
        card.setTitle("Fix login");
        card.setVersion(3);
    }

    @Test
    void movesCardAndPublishesEventWithFlushedVersion() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(boardListRepository.findById(newList.getId())).thenReturn(Optional.of(newList));
        User user = new User();
        user.setUsername("Bhavya");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        // A Mockito repository has no Hibernate behind it, so model the version bump on flush.
        doAnswer(invocation -> {
            card.setVersion(4);
            return null;
        }).when(cardRepository).flush();

        var response = cardService.moveCard(cardId, userId,
                new MoveCardRequest(newList.getId(), 3L));

        assertEquals(cardId, response.id());
        assertEquals(newList.getId(), response.listId());
        assertEquals(4, response.version());
        assertSame(newList, card.getList());
        verify(membershipService).requireMember(workspaceId, userId);
        verify(cardRepository).flush();

        ArgumentCaptor<CardMovedEvent> event = ArgumentCaptor.forClass(CardMovedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals(boardId, event.getValue().boardId());
        assertEquals(oldList.getId(), event.getValue().oldListId());
        assertEquals(newList.getId(), event.getValue().newListId());
        assertEquals(4, event.getValue().version());
        assertEquals("Bhavya", event.getValue().actorName());
    }

    @Test
    void staleClientVersionReturnsConflictWithoutWritingOrPublishing() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(boardListRepository.findById(newList.getId())).thenReturn(Optional.of(newList));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> cardService.moveCard(cardId, userId,
                        new MoveCardRequest(newList.getId(), 2L)));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertSame(oldList, card.getList());
        verify(cardRepository, never()).flush();
        verifyNoInteractions(eventPublisher, userRepository);
    }

    @Test
    void destinationOnAnotherBoardIsRejected() {
        Board otherBoard = new Board();
        otherBoard.setId(UUID.randomUUID());
        BoardList foreignList = list(UUID.randomUUID(), otherBoard, "Other");
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(boardListRepository.findById(foreignList.getId()))
                .thenReturn(Optional.of(foreignList));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> cardService.moveCard(cardId, userId,
                        new MoveCardRequest(foreignList.getId(), 3L)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertSame(oldList, card.getList());
        verify(cardRepository, never()).flush();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void nonMemberCannotLookUpDestinationOrMoveCard() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .when(membershipService).requireMember(workspaceId, userId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> cardService.moveCard(cardId, userId,
                        new MoveCardRequest(newList.getId(), 3L)));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(boardListRepository, eventPublisher);
        verify(cardRepository, never()).flush();
    }

    @Test
    void movingToCurrentListIsNoOp() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(boardListRepository.findById(oldList.getId()))
                .thenReturn(Optional.of(oldList));

        var response = cardService.moveCard(cardId, userId,
                new MoveCardRequest(oldList.getId(), 3L));

        assertEquals(3, response.version());
        assertSame(oldList, card.getList());
        verify(cardRepository, never()).flush();
        verifyNoInteractions(eventPublisher, userRepository);
    }

    @Test
    void databaseVersionConflictDoesNotPublishEvent() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(boardListRepository.findById(newList.getId())).thenReturn(Optional.of(newList));
        User user = new User();
        user.setUsername("Bhavya");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        ObjectOptimisticLockingFailureException conflict =
                new ObjectOptimisticLockingFailureException(Card.class, cardId);
        doThrow(conflict).when(cardRepository).flush();

        assertSame(conflict, assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> cardService.moveCard(cardId, userId,
                        new MoveCardRequest(newList.getId(), 3L))));

        verify(eventPublisher, never()).publishEvent(any());
    }

    private static BoardList list(UUID id, Board board, String name) {
        BoardList list = new BoardList();
        list.setId(id);
        list.setBoard(board);
        list.setName(name);
        return list;
    }
}
