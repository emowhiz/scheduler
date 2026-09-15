package com.example.scheduler.meeting;

import com.example.scheduler.AbstractIntegrationTest;
import com.example.scheduler.meeting.domain.CreateMeetingRequest;
import com.example.scheduler.slot.domain.CreateSlotRequest;
import com.example.scheduler.slot.domain.SlotResponse;
import com.example.scheduler.user.repository.CalendarEntity;
import com.example.scheduler.user.repository.CalendarRepository;
import com.example.scheduler.user.repository.UserEntity;
import com.example.scheduler.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingConcurrencyTest extends AbstractIntegrationTest {

    private static final int CONCURRENT_REQUESTS = 10;

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CalendarRepository calendarRepository;

    @Test
    void onlyOneOfManyConcurrentBookingsOnTheSameSlotSucceeds() throws InterruptedException {
        UUID organizerId = createUser();
        UUID slotId = createFreeSlot(
                organizerId, Instant.parse("2026-09-01T09:00:00Z"), Instant.parse("2026-09-01T09:30:00Z"));

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch startSignal = new CountDownLatch(1);
        AtomicInteger created = new AtomicInteger();
        AtomicInteger conflicted = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        try {
            List<Future<HttpStatus>> futures = IntStream.range(0, CONCURRENT_REQUESTS)
                    .mapToObj(i -> executor.submit(() -> {
                        startSignal.await();
                        ResponseEntity<String> response = restTemplate.postForEntity(
                                "/api/v1/meetings/organizer/{organizerId}/slot/{slotId}",
                                new CreateMeetingRequest("Race #" + i, null, List.of()),
                                String.class,
                                organizerId,
                                slotId);
                        return HttpStatus.valueOf(response.getStatusCode().value());
                    }))
                    .toList();

            startSignal.countDown();

            for (Future<HttpStatus> future : futures) {
                HttpStatus status = awaitResult(future);
                if (status == HttpStatus.CREATED) {
                    created.incrementAndGet();
                } else if (status == HttpStatus.CONFLICT) {
                    conflicted.incrementAndGet();
                } else {
                    other.incrementAndGet();
                }
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }

        assertThat(other.get()).as("requests that were neither 201 nor 409").isZero();
        assertThat(created.get()).isEqualTo(1);
        assertThat(conflicted.get()).isEqualTo(CONCURRENT_REQUESTS - 1);
    }

    private static HttpStatus awaitResult(Future<HttpStatus> future) {
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
}
