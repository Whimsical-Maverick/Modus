package com.tracker.realtime.events;

import java.util.UUID;

public record CardMovedEvent(UUID boardId, UUID cardId, UUID targetListId) {
}

