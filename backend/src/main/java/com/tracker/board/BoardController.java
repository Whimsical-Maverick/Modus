package com.tracker.board;

import com.tracker.board.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class BoardController
{
    private final BoardService boardService;

    @PostMapping("/workspaces/{workspaceId}/boards")
    public ResponseEntity<BoardResponse> createBoard(@PathVariable UUID workspaceId, @Valid @RequestBody CreateBoardRequest request,@AuthenticationPrincipal UUID currentUserId)
    {
        BoardResponse response =
                boardService.createBoard(workspaceId, currentUserId, request);

        return ResponseEntity
                .created(URI.create("/api/v1/boards/" + response.id()))
                .body(response);
    }

    @PostMapping("/boards/{boardId}/lists")
    public ResponseEntity<BoardListResponse> createList(
            @PathVariable UUID boardId,
            @Valid @RequestBody CreateBoardListRequest request,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        BoardListResponse response =
                boardService.createList(boardId, currentUserId, request);

        return ResponseEntity
                .created(URI.create("/api/v1/board-lists/" + response.id()))
                .body(response);
    }

    @GetMapping("/boards/{boardId}")
    public BoardDetailsResponse getBoard(
            @PathVariable UUID boardId,
            @AuthenticationPrincipal UUID currentUserId){

        return boardService.getBoard(boardId, currentUserId);
    }
}

