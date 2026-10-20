package com.tracker.board;

import com.tracker.board.dto.*;
import com.tracker.card.CardRepository;
import com.tracker.card.dto.CardResponse;
import com.tracker.activity.ActivityLogRepository;
import com.tracker.realtime.events.BoardContentDeletedEvent;
import com.tracker.workspace.Workspace;
import com.tracker.workspace.WorkspaceMembershipService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoardService
{
    private final BoardRepository boardRepository;
    private final BoardListRepository boardListRepository;
    private final WorkspaceMembershipService membershipService;
    private final CardRepository cardRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void deleteBoard(UUID boardId, UUID currentUserId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Board not found"));
        membershipService.requireOwner(board.getWorkspace().getId(), currentUserId);

        activityLogRepository.deleteByBoardId(boardId);
        boardRepository.delete(board);
        boardRepository.flush();
        eventPublisher.publishEvent(new BoardContentDeletedEvent(boardId, "BOARD_DELETED", boardId));
    }

    @Transactional
    public void deleteList(UUID listId, UUID currentUserId) {
        BoardList list = boardListRepository.findById(listId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        UUID boardId = list.getBoard().getId();
        membershipService.requireOwner(list.getBoard().getWorkspace().getId(), currentUserId);

        boardListRepository.delete(list);
        boardListRepository.flush();
        eventPublisher.publishEvent(new BoardContentDeletedEvent(boardId, "LIST_DELETED", listId));
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<BoardResponse> listBoards(UUID workspaceId, UUID currentUserId) {
        membershipService.requireMember(workspaceId, currentUserId);
        return boardRepository.findByWorkspace_IdOrderByNameAsc(workspaceId)
                .stream()
                .map(board -> new BoardResponse(
                        board.getId(), workspaceId, board.getName()))
                .toList();
    }

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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public BoardDetailsResponse getBoard(UUID boardId, UUID currentUserId)
    {
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Board not found"));

        membershipService.requireMember(board.getWorkspace().getId(),currentUserId);

        var cardsByList = cardRepository.findByList_Board_Id(boardId)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        card -> card.getList().getId(),
                        java.util.stream.Collectors.mapping(
                                card -> new CardResponse(
                                        card.getId(),
                                        card.getList().getId(),
                                        card.getTitle(),
                                        card.getVersion()
                                ),
                                java.util.stream.Collectors.toList()
                        )
                ));

        var lists = boardListRepository
                .findByBoard_IdOrderBySortOrderAsc(boardId)
                .stream()
                .map(list -> new BoardDetailsResponse.ListDetails(
                        list.getId(),
                        list.getName(),
                        list.getSortOrder(),
                        cardsByList.getOrDefault(
                                list.getId(),
                                java.util.List.of()
                        )
                ))
                .toList();

        return new BoardDetailsResponse(board.getId(),board.getName(),lists);
    }


}

