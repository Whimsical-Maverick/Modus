package com.tracker.workspace.dto;

import java.time.Instant;
import java.util.UUID;

public record ArchivedWorkspaceResponse(UUID id, String name, Instant archivedAt) {}
