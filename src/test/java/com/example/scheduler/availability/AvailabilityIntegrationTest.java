package com.example.scheduler.availability;

import com.example.scheduler.AbstractIntegrationTest;
import com.example.scheduler.availability.domain.AvailabilityResponse;
import com.example.scheduler.availability.domain.CommonAvailabilityResponse;
import com.example.scheduler.availability.domain.Interval;
import com.example.scheduler.meeting.domain.CreateMeetingRequest;
import com.example.scheduler.meeting.domain.MeetingResponse;
import com.example.scheduler.slot.domain.CreateSlotRequest;
import com.example.scheduler.slot.domain.SlotResponse;
import com.example.scheduler.user.repository.CalendarEntity;
import com.example.scheduler.user.repository.CalendarRepository;
import com.example.scheduler.user.repository.UserEntity;
import com.example.scheduler.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CalendarRepository calendarRepository;

    private UUID userA;
    private UUID userB;

    @BeforeEach
    void createUsers() {
        userA = createUser();
        userB = createUser();
    }

    private UUID createUser() {
        UserEntity user = new UserEntity();
        user.setEmail(UUID.randomUUID() + "x@x.com");
        UUID userId = userRepository.save(user).getId();
        CalendarEntity calendar = new CalendarEntity();
        calendar.setUserId(userId);
        calendarRepository.save(calendar);
        return userId;
    }

    private UUID createFreeSlot(UUID userId, Instant start, Instant end) {
        ResponseEntity<SlotResponse> response = restTemplate.postForEntity(
                "/api/v1/slots", new CreateSlotRequest(userId, start, end), SlotResponse.class);
        return response.getBody().id();
    }

    private AvailabilityResponse getAvailability(UUID userId, String from, String to) {
        String url = UriComponentsBuilder.fromPath("/api/v1/availability/users/{userId}")
                .queryParam("from", from)
                .queryParam("to", to)
                .buildAndExpand(userId)
                .toUriString();
        return restTemplate.getForObject(url, AvailabilityResponse.class);
    }
    @Test
    void availability_reflectsFreeAndBusySlots() {
        createFreeSlot(userA, Instant.parse("2026-07-01T09:00:00Z"), Instant.parse("2026-07-01T17:00:00Z"));
        UUID busySlotId =
                createFreeSlot(userA, Instant.parse("2026-07-01T10:00:00Z"), Instant.parse("2026-07-01T10:30:00Z"));
        ResponseEntity<MeetingResponse> focusBlock = restTemplate.postForEntity(
                "/api/v1/meetings/organizer/{organizerId}/slot/{slotId}",
                new CreateMeetingRequest("Focus block", null, List.of()),
                MeetingResponse.class,
                userA,
                busySlotId);

        AvailabilityResponse availability =
                getAvailability(userA, "2026-07-01T00:00:00Z", "2026-07-02T00:00:00Z");

        assertThat(availability.busy()).contains(new Interval(
                Instant.parse("2026-07-01T10:00:00Z"), Instant.parse("2026-07-01T10:30:00Z")));
        assertThat(availability.free())
                .anyMatch(i -> i.start().equals(Instant.parse("2026-07-01T09:00:00Z"))
                        && i.end().equals(Instant.parse("2026-07-01T10:00:00Z")));
    }

    @Test
    void commonAvailability_excludesBookedWindowForBothUsers() {
        UUID slotA = createFreeSlot(userA, Instant.parse("2026-07-05T09:00:00Z"), Instant.parse("2026-07-05T17:00:00Z"));
        createFreeSlot(userB, Instant.parse("2026-07-05T09:00:00Z"), Instant.parse("2026-07-05T17:00:00Z"));

        restTemplate.postForEntity(
                "/api/v1/meetings/organizer/{organizerId}/slot/{slotId}",
                new CreateMeetingRequest("Cross-team sync", null, List.of(userB)),
                MeetingResponse.class,
                userA,
                slotA);

        String url = UriComponentsBuilder.fromPath("/api/v1/availability")
                .queryParam("userIds", userA + "," + userB)
                .queryParam("from", "2026-07-05T00:00:00Z")
                .queryParam("to", "2026-07-06T00:00:00Z")
                .queryParam("durationMinutes", 30)
                .build()
                .toUriString();

        ResponseEntity<CommonAvailabilityResponse> response =
                restTemplate.getForEntity(url, CommonAvailabilityResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // both users only ever declared 09:00-17:00 as free, and that whole window is now booked
        assertThat(response.getBody().commonFree()).isEmpty();
    }

    @Test
    void availability_rejectsWindowLargerThanConfiguredMax() {
        String url = UriComponentsBuilder.fromPath("/api/v1/availability/users/{userId}")
                .queryParam("from", "2026-01-01T00:00:00Z")
                .queryParam("to", "2027-01-01T00:00:00Z")
                .buildAndExpand(userA)
                .toUriString();

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
