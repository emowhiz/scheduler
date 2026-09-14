package com.example.scheduler.slot.domain;

import com.example.scheduler.slot.repository.SlotStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record UpdateSlotRequest(@NotNull UUID userId, Instant startAt, Instant endAt, SlotStatus status) {
}