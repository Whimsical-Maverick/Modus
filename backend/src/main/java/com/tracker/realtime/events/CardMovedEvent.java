package com.tracker.realtime.events;

import java.util.UUID;

public record CardMovedEvent(UUID boardId,
                             UUID cardId,
                             UUID oldListId,
                             UUID newListId,
                             long version,
                             UUID actorId,
                             String actorName,
                             String cardTitle,
                             String oldListName,
                             String newListName)
{
}

