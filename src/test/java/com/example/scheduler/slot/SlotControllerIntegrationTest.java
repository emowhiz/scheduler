package com.example.scheduler.slot;

import com.example.scheduler.AbstractIntegrationTest;
import com.example.scheduler.slot.domain.CreateSlotRequest;
import com.example.scheduler.slot.domain.SlotResponse;
import com.example.scheduler.slot.domain.UpdateSlotRequest;
import com.example.scheduler.slot.repository.SlotStatus;
import com.example.scheduler.slot.repository.TimeSlotRepository;
import com.example.scheduler.user.repository.CalendarEntity;
import com.example.scheduler.user.repository.CalendarRepository;
import com.example.scheduler.user.repository.UserEntity;
import com.example.scheduler.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SlotControllerIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CalendarRepository calendarRepository;
    @Autowired
    private TimeSlotRepository slotRepository;

    private UUID userId;

    @BeforeEach
    void createUser() {
        UserEntity user = new UserEntity();
        user.setEmail(UUID.randomUUID() +"x@x.com");
        userId= userRepository.save(user).getId();
        CalendarEntity calendar = new CalendarEntity();
        calendar.setUserId(userId);
        calendarRepository.save(calendar);
    }

    @Test
    void createSlot() {
        Instant start = Instant.parse("2026-02-01T09:00:00Z");
        Instant end = Instant.parse("2026-02-01T10:00:00Z");
        ResponseEntity<SlotResponse> createResponse = restTemplate.postForEntity(
                "/api/v1/slots", new CreateSlotRequest(userId, start, end), SlotResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody().status()).isEqualTo(SlotStatus.FREE);
        assertThat(createResponse.getBody().createdAt()).isNotNull();
        assertThat(createResponse.getBody().updatedAt()).isNotNull();
    }
    @Test
    void createSlotWithInvalidRangeShouldBeBadRequest() {
        Instant start = Instant.parse("2026-02-01T09:00:00Z");
        Instant end = Instant.parse("2026-02-01T10:00:00Z");
        ResponseEntity<String> createResponse = restTemplate.postForEntity(
                "/api/v1/slots", new CreateSlotRequest(userId, end, start), String.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deleteSlot_removesIt() {
        ResponseEntity<SlotResponse> createResponse = restTemplate.postForEntity(
                "/api/v1/slots",
                new CreateSlotRequest(userId, Instant.parse("2026-04-01T09:00:00Z"), Instant.parse("2026-04-01T10:00:00Z")),
                SlotResponse.class);
        UUID slotId = createResponse.getBody().id();
        assertThat(slotRepository.findById(slotId)).isPresent();
        restTemplate.delete("/api/v1/slots/{slotId}", slotId);
        assertThat(slotRepository.findById(slotId)).isNotPresent();
    }
    @Test
    void markingTwoOverlappingSlotsBusy_secondOneConflicts() {
        Instant start = Instant.parse("2026-05-01T09:00:00Z");
        Instant end = Instant.parse("2026-05-01T10:00:00Z");

        ResponseEntity<SlotResponse> slotA = restTemplate.postForEntity(
                "/api/v1/slots", new CreateSlotRequest(userId,start, end), SlotResponse.class);
        ResponseEntity<SlotResponse> slotB = restTemplate.postForEntity(
                "/api/v1/slots", new CreateSlotRequest(userId,start, end), SlotResponse.class);

        ResponseEntity<SlotResponse> markBusyA = restTemplate.exchange(
                "/api/v1/slots/{slotId}",
                HttpMethod.PATCH,
                new HttpEntity<>(new UpdateSlotRequest(userId, null,null, SlotStatus.BUSY)),
                SlotResponse.class,
                slotA.getBody().id());
        assertThat(markBusyA.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(markBusyA.getBody().updatedAt()).isNotNull();

        ResponseEntity<String> markBusyB = restTemplate.exchange(
                "/api/v1/slots/{slotId}",
                HttpMethod.PATCH,
                new HttpEntity<>(new UpdateSlotRequest(userId, null,null, SlotStatus.BUSY)),
                String.class,
                slotB.getBody().id());
        assertThat(markBusyB.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}