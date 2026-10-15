package com.tracker.board.dto;

import java.util.UUID;

public record BoardResponse(UUID id, UUID workspaceId, String name)
{

}

