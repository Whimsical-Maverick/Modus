package com.tracker.activity;

import java.time.Instant;
import java.util.UUID;

public record ActivityLogResponse(
        UUID id, UUID cardId, String action, String description, Instant createdAt) {}
