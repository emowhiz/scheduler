package com.example.scheduler.meeting;

import com.example.scheduler.AbstractIntegrationTest;
import com.example.scheduler.meeting.domain.CreateMeetingRequest;
import com.example.scheduler.meeting.domain.MeetingResponse;
import com.example.scheduler.meeting.domain.UpdateMeetingRequest;
import com.example.scheduler.meeting.repository.MeetingRepository;
import com.example.scheduler.meeting.repository.MeetingStatus;
import com.example.scheduler.slot.domain.CreateSlotRequest;
import com.example.scheduler.slot.domain.SlotResponse;
import com.example.scheduler.slot.repository.SlotStatus;
import com.example.scheduler.slot.repository.TimeSlotEntity;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CalendarRepository calendarRepository;
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    @Autowired
    private MeetingRepository meetingRepository;

    private UUID organizerId;
    private UUID participantId;

    @BeforeEach
    void createUsers() {
        organizerId = createUser("organizer");
        participantId = createUser("participant");
    }

    private UUID createUser(String label) {
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

    @Test
    void bookMeeting_marksOrganizerSlotBusyAndMirrorsToParticipant() {
        Instant start = Instant.parse("2026-06-01T09:00:00Z");
        Instant end = Instant.parse("2026-06-01T09:30:00Z");
        UUID slotId = createFreeSlot(organizerId, start, end);

        ResponseEntity<MeetingResponse> bookResponse = restTemplate.postForEntity(
                "/api/v1/meetings/organizer/{organizerId}/slot/{slotId}",
                new CreateMeetingRequest("Design sync", "Discuss the API", List.of(participantId)),
                MeetingResponse.class,
                organizerId,
                slotId);

        assertThat(bookResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        MeetingResponse meeting = bookResponse.getBody();
        assertThat(meeting.status()).isEqualTo(MeetingStatus.SCHEDULED);
        assertThat(meeting.participants()).containsExactly(participantId);
        assertThat(meeting.createdAt()).isNotNull();
        assertThat(meeting.updatedAt()).isNotNull();

        assertThat(timeSlotRepository.findByMeetingId(meeting.id())).anySatisfy(
                timeSlot -> {
                    Optional<CalendarEntity> byUserId = calendarRepository.findByUserId(participantId);
                    assertThat(byUserId).isPresent();
                    assertThat(timeSlot.getStatus()).isEqualTo(SlotStatus.BUSY);
                    assertThat(timeSlot.getMeetingId()).isEqualTo(meeting.id());
                    assertThat(timeSlot.getCalendarId()).isEqualTo(byUserId.get().getId());
                });
    }

    @Test
    void bookMeeting_onAlreadyBusySlot_returns409() {
        Instant start = Instant.parse("2026-06-05T09:00:00Z");
        Instant end = Instant.parse("2026-06-05T09:30:00Z");
        UUID slotId = createFreeSlot(organizerId, start, end);
        restTemplate.postForEntity(
                "/api/v1/meetings/organizer/{organizerId}/slot/{slotId}",
                new CreateMeetingRequest("First booking", null, List.of()),
                MeetingResponse.class,
                organizerId,
                slotId);

        ResponseEntity<String> secondBooking = restTemplate.postForEntity(
                "/api/v1/meetings/organizer/{organizerId}/slot/{slotId}",
                new CreateMeetingRequest("Second booking", null, List.of()),
                String.class,
                organizerId,
                slotId);

        assertThat(secondBooking.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void bookMeeting_withBusyParticipant_returns409() {
        Instant start = Instant.parse("2026-06-10T09:00:00Z");
        Instant end = Instant.parse("2026-06-10T09:30:00Z");

        UUID participantSlotId = createFreeSlot(participantId, start, end);
        restTemplate.postForEntity(
                "/api/v1/meetings/organizer/{organizerId}/slot/{slotId}",
                new CreateMeetingRequest("Participant's own meeting", null, List.of()),
                MeetingResponse.class,
                participantId,
                participantSlotId);

        UUID organizerSlotId = createFreeSlot(organizerId, start, end);
        ResponseEntity<String> bookResponse = restTemplate.postForEntity(
                "/api/v1/meetings/organizer/{organizerId}/slot/{slotId}",
                new CreateMeetingRequest("Conflicting invite", null, List.of(participantId)),
                String.class,
                organizerId,
                organizerSlotId);

        assertThat(bookResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void cancelMeeting_freesOrganizerSlotAndRemovesParticipantMirror() {
        Instant start = Instant.parse("2026-06-15T09:00:00Z");
        Instant end = Instant.parse("2026-06-15T09:30:00Z");
        UUID slotId = createFreeSlot(organizerId, start, end);

        ResponseEntity<MeetingResponse> bookResponse = restTemplate.postForEntity(
                "/api/v1/meetings/organizer/{organizerId}/slot/{slotId}",
                new CreateMeetingRequest("Cancel me", null, List.of(participantId)),
                MeetingResponse.class,
                organizerId,
                slotId);
        UUID meetingId = bookResponse.getBody().id();

        Optional<TimeSlotEntity> organizerSlot = calendarRepository.findByUserId(organizerId)
                .flatMap(cal -> timeSlotRepository.findByMeetingIdAndCalendarId(meetingId, cal.getId()));
        Optional<TimeSlotEntity> participantSlot = calendarRepository.findByUserId(participantId)
                .flatMap(cal -> timeSlotRepository.findByMeetingIdAndCalendarId(meetingId, cal.getId()));

        assertThat(organizerSlot).isPresent();
        assertThat(participantSlot).isPresent();
        restTemplate.delete("/api/v1/meetings/{meetingId}", meetingId);

        assertThat(timeSlotRepository.findById(organizerSlot.get().getId())).matches(
                probableSlot -> probableSlot.isPresent()
                        && probableSlot.get().getStatus().equals(SlotStatus.FREE));

        assertThat(timeSlotRepository.findById(participantSlot.get().getId())).isEmpty();

        ResponseEntity<MeetingResponse> cancelled =
                restTemplate.getForEntity("/api/v1/meetings/{meetingId}", MeetingResponse.class, meetingId);
        assertThat(cancelled.getBody().status()).isEqualTo(MeetingStatus.CANCELLED);
    }

    @Test
    void updateMeeting_addsAndRemovesParticipants() {
        Instant start = Instant.parse("2026-06-20T09:00:00Z");
        Instant end = Instant.parse("2026-06-20T09:30:00Z");
        UUID slotId = createFreeSlot(organizerId, start, end);
        UUID secondParticipantId = createUser("second");

        ResponseEntity<MeetingResponse> bookResponse = restTemplate.postForEntity(
                "/api/v1/meetings/organizer/{organizerId}/slot/{slotId}",
                new CreateMeetingRequest("Evolving meeting", null, List.of(participantId)),
                MeetingResponse.class,
                organizerId,
                slotId);
        UUID meetingId = bookResponse.getBody().id();

        ResponseEntity<MeetingResponse> updateResponse = restTemplate.exchange(
                "/api/v1/meetings/{meetingId}",
                HttpMethod.PATCH,
                new HttpEntity<>(new UpdateMeetingRequest(null, null, List.of(secondParticipantId))),
                MeetingResponse.class,
                meetingId);

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().participants())
                .containsExactly(secondParticipantId);

        assertThat(timeSlotRepository.findByMeetingId(meetingId)).noneSatisfy(
                timeSlot -> {
                    Optional<CalendarEntity> byUserId = calendarRepository.findByUserId(participantId);
                    assertThat(byUserId).isPresent();
                    assertThat(timeSlot.getStatus()).isEqualTo(SlotStatus.BUSY);
                    assertThat(timeSlot.getMeetingId()).isEqualTo(meetingId);
                    assertThat(timeSlot.getCalendarId()).isEqualTo(byUserId.get().getId());
                });
    }
}
