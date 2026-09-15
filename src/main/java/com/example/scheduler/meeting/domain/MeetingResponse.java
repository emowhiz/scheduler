package com.example.scheduler.meeting.domain;

import com.example.scheduler.meeting.repository.MeetingEntity;
import com.example.scheduler.meeting.repository.MeetingStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MeetingResponse(
        UUID id,
        UUID organizerUserId,
        String title,
        String description,
        Instant startAt,
        Instant endAt,
        MeetingStatus status,
        List<UUID> participants,
        Instant createdAt,
        Instant updatedAt) {

    public static MeetingResponse from(MeetingEntity meeting, List<UUID> participants) {
        return new MeetingResponse(
                meeting.getId(),
                meeting.getOrganizerUserId(),
                meeting.getTitle(),
                meeting.getDescription(),
                meeting.getStartAt(),
                meeting.getEndAt(),
                meeting.getStatus(),
                participants,
                meeting.getCreatedAt(),
                meeting.getUpdatedAt());
    }
}
