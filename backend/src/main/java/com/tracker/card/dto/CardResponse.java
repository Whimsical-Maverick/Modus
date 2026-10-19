package com.tracker.card.dto;

import java.util.UUID;

public record CardResponse(UUID id, UUID listId,String title,long version)
{

}
