package com.tracker.activity;

import com.tracker.board.BoardRepository;
import com.tracker.workspace.WorkspaceMembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/boards/{boardId}/activity")
public class ActivityLogController {
    private final BoardRepository boardRepository;
    private final WorkspaceMembershipService membershipService;
    private final ActivityLogRepository activityLogRepository;

    @GetMapping
    public List<ActivityLogResponse> list(
            @PathVariable UUID boardId,
            @AuthenticationPrincipal UUID currentUserId) {
        UUID workspaceId = boardRepository.findWorkspaceIdByBoardId(boardId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Board not found"));
        membershipService.requireMember(workspaceId, currentUserId);
        return activityLogRepository.findTop50ByBoardIdOrderByCreatedAtDesc(boardId)
                .stream()
                .map(log -> new ActivityLogResponse(
                        log.getId(), log.getCardId(), log.getAction(),
                        log.getDescription(), log.getCreatedAt()))
                .toList();
    }
}
