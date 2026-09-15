package com.example.scheduler.availability;

import com.example.scheduler.availability.domain.AvailabilityResponse;
import com.example.scheduler.availability.domain.CommonAvailabilityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/api/v1/availability/users/{userId}")
    public AvailabilityResponse getAvailability(
            @PathVariable UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return availabilityService.getAvailability(userId, from, to);
    }

    @GetMapping("/api/v1/availability")
    public CommonAvailabilityResponse getCommonAvailability(
            @RequestParam List<UUID> userIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer durationMinutes) {
        return availabilityService.getCommonAvailability(userIds, from, to, durationMinutes);
    }
}
