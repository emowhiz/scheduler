package com.example.scheduler.meeting.domain;

import java.util.List;
import java.util.UUID;

public record UpdateMeetingRequest(String title, String description, List<UUID> participantUserIds) {
}
