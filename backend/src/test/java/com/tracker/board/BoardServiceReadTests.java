package com.tracker.board;

import com.tracker.card.Card;
import com.tracker.card.CardRepository;
import com.tracker.workspace.Workspace;
import com.tracker.workspace.WorkspaceMembershipService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardServiceReadTests {
    @Mock BoardRepository boardRepository;
    @Mock BoardListRepository boardListRepository;
    @Mock WorkspaceMembershipService membershipService;
    @Mock CardRepository cardRepository;
    @InjectMocks BoardService boardService;

    @Test
    void memberGetsListsAndCardVersions() {
        UUID boardId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Board board = board(boardId, workspaceId);
        BoardList todo = list(board, "To Do", 0);
        BoardList done = list(board, "Done", 1);
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setList(todo);
        card.setTitle("Fix login");
        card.setVersion(3);
        when(boardRepository.findById(boardId)).thenReturn(Optional.of(board));
        when(cardRepository.findByList_Board_Id(boardId)).thenReturn(List.of(card));
        when(boardListRepository.findByBoard_IdOrderBySortOrderAsc(boardId))
                .thenReturn(List.of(todo, done));

        var result = boardService.getBoard(boardId, userId);

        verify(membershipService).requireMember(workspaceId, userId);
        assertEquals(boardId, result.id());
        assertEquals(2, result.lists().size());
        assertEquals(card.getId(), result.lists().get(0).cards().get(0).id());
        assertEquals(3, result.lists().get(0).cards().get(0).version());
        assertEquals(0, result.lists().get(1).cards().size());
    }

    @Test
    void nonMemberCannotLoadListsOrCards() {
        UUID boardId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(boardRepository.findById(boardId))
                .thenReturn(Optional.of(board(boardId, workspaceId)));
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .when(membershipService).requireMember(workspaceId, userId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> boardService.getBoard(boardId, userId));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(boardListRepository, cardRepository);
    }

    private static Board board(UUID boardId, UUID workspaceId) {
        Workspace workspace = new Workspace();
        workspace.setId(workspaceId);
        Board board = new Board();
        board.setId(boardId);
        board.setName("Project");
        board.setWorkspace(workspace);
        return board;
    }

    private static BoardList list(Board board, String name, int sortOrder) {
        BoardList list = new BoardList();
        list.setId(UUID.randomUUID());
        list.setBoard(board);
        list.setName(name);
        list.setSortOrder(sortOrder);
        return list;
    }
}
