package com.tracker.board;

import com.tracker.board.dto.BoardListResponse;
import com.tracker.board.dto.BoardResponse;
import com.tracker.board.dto.CreateBoardListRequest;
import com.tracker.board.dto.CreateBoardRequest;
import com.tracker.workspace.Workspace;
import com.tracker.workspace.WorkspaceMembershipService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoardService
{
    private final BoardRepository boardRepository;
    private final BoardListRepository boardListRepository;
    private final WorkspaceMembershipService membershipService;

    @Transactional
    public BoardResponse createBoard(UUID workspaceId, UUID currentUserId, CreateBoardRequest request)
    {
        Workspace workspace = membershipService.requireMember(workspaceId,currentUserId).getWorkspace();
        Board board = new Board();
        board.setWorkspace(workspace);
        board.setName(request.name().trim());

        Board saved = boardRepository.save(board);
        return new BoardResponse(saved.getId(),workspace.getId(),saved.getName());
    }

    @Transactional
    public BoardListResponse createList(
            UUID boardId,
            UUID currentUserId,
            CreateBoardListRequest request
    )
    {
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Board not found"));

        membershipService.requireMember(board.getWorkspace().getId(),currentUserId);

        if(boardListRepository.existsByBoard_IdAndSortOrder(boardId,request.sortOrder())){
            throw new ResponseStatusException(HttpStatus.CONFLICT,"List Position is already occupied");
        }

        BoardList list = new BoardList();
        list.setBoard(board);
        list.setName(request.name().trim());
        list.setSortOrder(request.sortOrder());

        try {
            BoardList saved = boardListRepository.saveAndFlush(list);

            return new BoardListResponse(
                    saved.getId(),
                    boardId,
                    saved.getName(),
                    saved.getSortOrder()
            );
        } catch (DataIntegrityViolationException ex) {
            // Database constraint protects against simultaneous inserts.
            if (ex.getMostSpecificCause() instanceof SQLException sql
                    && "23505".equals(sql.getSQLState())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "List position is already occupied",
                        ex
                );
            }
            throw ex;
        }
    }

}

