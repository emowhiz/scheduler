package com.example.scheduler.slot.domain;

import com.example.scheduler.slot.repository.SlotStatus;
import com.example.scheduler.slot.repository.TimeSlotEntity;

import java.time.Instant;
import java.util.UUID;

public record SlotResponse(
        UUID id,
        UUID userId,
        Instant startAt,
        Instant endAt,
        SlotStatus status,
        UUID meetingId,
        Instant createdAt,
        Instant updatedAt) {

    public static SlotResponse from(TimeSlotEntity slot, UUID userId) {
        return new SlotResponse(
                slot.getId(),
                userId,
                slot.getStartAt(),
                slot.getEndAt(),
                slot.getStatus(),
                slot.getMeetingId(),
                slot.getCreatedAt(),
                slot.getUpdatedAt());
    }
}