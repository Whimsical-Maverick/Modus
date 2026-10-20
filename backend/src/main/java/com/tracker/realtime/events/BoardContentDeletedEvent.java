package com.tracker.realtime.events;

import java.util.UUID;

public record BoardContentDeletedEvent(UUID boardId, String type, UUID entityId) {
}
