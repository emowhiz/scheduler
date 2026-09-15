package com.example.scheduler.meeting.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateMeetingRequest(@NotBlank String title,
                                   String description,
                                   List<UUID> participantUserIds) {
    public List<UUID> participantsOrEmpty() {
        return participantUserIds == null ? List.of() : participantUserIds;
    }
}
