package com.tracker.board.dto;

import com.tracker.card.dto.CardResponse;

import java.util.List;
import java.util.UUID;

public record BoardDetailsResponse(UUID id, String name,List<ListDetails> list)
{
    public record ListDetails(UUID id, String name, int sortOrder, List<CardResponse>Cards){}
}
