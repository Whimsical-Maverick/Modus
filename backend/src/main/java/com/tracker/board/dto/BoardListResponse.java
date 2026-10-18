package com.tracker.board.dto;

import java.util.UUID;

public record BoardListResponse(UUID id, UUID boardId, String name, Integer sortOrder)
{

}
