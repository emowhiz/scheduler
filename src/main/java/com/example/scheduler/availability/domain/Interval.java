package com.example.scheduler.availability.domain;

import java.time.Instant;

public record Interval(Instant start, Instant end) {

    public Interval {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("end must be after start");
        }
    }
}
