package com.example.scheduler.availability;

import com.example.scheduler.availability.domain.AvailabilityResponse;
import com.example.scheduler.availability.domain.CommonAvailabilityResponse;
import com.example.scheduler.availability.domain.Interval;
import com.example.scheduler.availability.domain.IntervalMath;
import com.example.scheduler.config.AppConfig;
import com.example.scheduler.slot.repository.TimeSlotEntity;
import com.example.scheduler.slot.repository.TimeSlotRepository;
import com.example.scheduler.user.CalendarService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private static final int MAX_USERS_FOR_COMMON = 50;

    private final TimeSlotRepository timeSlotRepository;
    private final CalendarService calendarService;
    private final MeterRegistry meterRegistry;
    private final AppConfig appProperties;


    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(UUID userId, Instant from, Instant to) {
        return meterRegistry
                .timer("availability.query.duration", "endpoint", "single")
                .record(() -> {
                    requireValidWindow(from, to);
                    UUID calendarId = calendarService.getCalendarIdForUser(userId);
                    List<TimeSlotEntity> slots =
                            timeSlotRepository.findAllOverlappingForCalendars(List.of(calendarId), from, to);
                    SlotDistribution slotDistribution = computeFreeBusy(slots, from, to);
                    return new AvailabilityResponse(userId, from, to, slotDistribution.free(), slotDistribution.busy());
                });
    }

    @Transactional(readOnly = true)
    public CommonAvailabilityResponse getCommonAvailability(
            List<UUID> userIds, Instant from, Instant to, Integer durationMinutes) {
        return meterRegistry
                .timer("availability.query.duration", "endpoint", "common")
                .record(() -> {
                    if (userIds.isEmpty()) {
                        throw new IllegalArgumentException("At least one userId is required");
                    }
                    if (userIds.size() > MAX_USERS_FOR_COMMON) {
                        throw new IllegalArgumentException(
                                "Too many users requested (max " + MAX_USERS_FOR_COMMON + ")");
                    }
                    requireValidWindow(from, to);

                    Map<UUID, UUID> userToCalendar = new LinkedHashMap<>();
                    for (UUID userId : userIds) {
                        userToCalendar.put(userId, calendarService.getCalendarIdForUser(userId));
                    }

                    List<TimeSlotEntity> allSlots = timeSlotRepository.findAllOverlappingForCalendars(
                            new ArrayList<>(userToCalendar.values()), from, to);
                    Map<UUID, List<TimeSlotEntity>> byCalendar = groupByCalendar(allSlots);

                    List<List<Interval>> perUserFree = new ArrayList<>();
                    for (UUID calendarId : userToCalendar.values()) {
                        SlotDistribution slotDistribution =
                                computeFreeBusy(byCalendar.getOrDefault(calendarId, List.of()), from, to);
                        perUserFree.add(slotDistribution.free());
                    }

                    List<Interval> common = IntervalMath.intersectAll(perUserFree);
                    if (durationMinutes != null) {
                        common = IntervalMath.atLeast(common, Duration.ofMinutes(durationMinutes));
                    }
                    return new CommonAvailabilityResponse(userIds, from, to, durationMinutes, common);
                });
    }

    private SlotDistribution computeFreeBusy(List<TimeSlotEntity> slots, Instant from, Instant to) {
        Interval window = new Interval(from, to);
        List<Interval> free = slots.stream()
                .filter(TimeSlotEntity::isFree)
                .map(s -> new Interval(s.getStartAt(), s.getEndAt()))
                .toList();
        List<Interval> busy = slots.stream()
                .filter(s -> !s.isFree())
                .map(s -> new Interval(s.getStartAt(), s.getEndAt()))
                .toList();

        List<Interval> mergedBusy = IntervalMath.clip(IntervalMath.merge(busy), window);
        List<Interval> mergedFree =
                IntervalMath.clip(IntervalMath.subtract(IntervalMath.merge(free), mergedBusy), window);
        return new SlotDistribution(mergedFree, mergedBusy);
    }

    private Map<UUID, List<TimeSlotEntity>> groupByCalendar(List<TimeSlotEntity> slots) {
        return slots.stream().collect(Collectors.groupingBy(TimeSlotEntity::getCalendarId));
    }

    private void requireValidWindow(Instant from, Instant to) {
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("'to' must be after 'from'");
        }
        int maxDays = appProperties.availability().maxWindowDays();
        if (Duration.between(from, to).compareTo(Duration.ofDays(maxDays)) > 0) {
            throw new IllegalArgumentException("Requested window exceeds the maximum of " + maxDays + " days");
        }
    }

    private record SlotDistribution(List<Interval> free, List<Interval> busy) {
    }
}
