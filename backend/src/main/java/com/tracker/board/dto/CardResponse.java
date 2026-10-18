package com.tracker.board.dto;

import java.util.UUID;

public record CardResponse(UUID id, UUID listId,String title)
{

}
