package com.tracker.realtime.events;

import java.util.UUID;

public record CardCreatedEvent(UUID boardId, UUID cardId) {
}

