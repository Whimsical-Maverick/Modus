package com.tracker.card;

import com.tracker.board.Board;
import com.tracker.board.BoardList;
import com.tracker.board.BoardListRepository;
import com.tracker.realtime.events.BoardContentDeletedEvent;
import com.tracker.user.UserRepository;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceDeleteTests {
    @Mock BoardListRepository boardListRepository;
    @Mock CardRepository cardRepository;
    @Mock WorkspaceMembershipService membershipService;
    @Mock UserRepository userRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks CardService service;

    @Test
    void memberDeletesCardAndPublishesAfterFlush() {
        UUID userId = UUID.randomUUID();
        Card card = card();
        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));

        service.deleteCard(card.getId(), userId);

        var order = inOrder(membershipService, cardRepository, eventPublisher);
        order.verify(membershipService).requireMember(
                card.getList().getBoard().getWorkspace().getId(), userId);
        order.verify(cardRepository).delete(card);
        order.verify(cardRepository).flush();
        order.verify(eventPublisher).publishEvent(new BoardContentDeletedEvent(
                card.getList().getBoard().getId(), "CARD_DELETED", card.getId()));
    }

    @Test
    void nonMemberCannotDeleteCard() {
        UUID userId = UUID.randomUUID();
        Card card = card();
        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(membershipService).requireMember(
                        card.getList().getBoard().getWorkspace().getId(), userId);

        var error = assertThrows(ResponseStatusException.class,
                () -> service.deleteCard(card.getId(), userId));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        org.mockito.Mockito.verify(cardRepository, org.mockito.Mockito.never()).delete(card);
        verifyNoInteractions(eventPublisher);
    }

    private static Card card() {
        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        Board board = new Board();
        board.setId(UUID.randomUUID());
        board.setWorkspace(workspace);
        BoardList list = new BoardList();
        list.setId(UUID.randomUUID());
        list.setBoard(board);
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setList(list);
        return card;
    }
}
