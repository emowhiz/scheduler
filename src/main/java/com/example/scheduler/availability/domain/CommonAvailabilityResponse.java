package com.example.scheduler.availability.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CommonAvailabilityResponse(
        List<UUID> userIds, Instant from, Instant to, Integer durationMinutes, List<Interval> commonFree) {
}
