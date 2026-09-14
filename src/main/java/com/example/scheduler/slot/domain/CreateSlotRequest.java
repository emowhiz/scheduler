package com.example.scheduler.slot.domain;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateSlotRequest(@NotNull UUID userId, @NotNull Instant startAt, @NotNull Instant endAt) {
}
