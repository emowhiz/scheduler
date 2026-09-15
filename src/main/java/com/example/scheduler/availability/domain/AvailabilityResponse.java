package com.example.scheduler.availability.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AvailabilityResponse(UUID userId, Instant from, Instant to, List<Interval> free, List<Interval> busy) {
}
