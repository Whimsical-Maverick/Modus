package com.tracker.card.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MoveCardRequest(@NotNull UUID newListId, @NotNull @Min(0) Long expectedVersion) {

}
