package com.example.scheduler.slot;

import com.example.scheduler.slot.domain.CreateSlotRequest;
import com.example.scheduler.slot.domain.SlotResponse;
import com.example.scheduler.slot.domain.UpdateSlotRequest;
import com.example.scheduler.slot.repository.SlotStatus;
import com.example.scheduler.slot.repository.TimeSlotEntity;
import com.example.scheduler.slot.repository.TimeSlotRepository;
import com.example.scheduler.user.CalendarService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlotService {
    private final TimeSlotRepository timeSlotRepository;
    private final CalendarService calendarService;
    private final MeterRegistry meterRegistry;

    public SlotResponse createSlot(CreateSlotRequest request) {
        requireValidRange(request.startAt(), request.endAt());
        UUID calendarId = calendarService.getCalendarIdForUser(request.userId());
        TimeSlotEntity slot = timeSlotRepository.save(new TimeSlotEntity(calendarId, request.startAt(), request.endAt()));
        meterRegistry.counter("slot.creation").increment();
        return SlotResponse.from(slot, request.userId());
    }

    @Transactional
    public SlotResponse updateSlot(UUID slotId, UpdateSlotRequest request) {
        UUID userId = request.userId();
        UUID calendarId = calendarService.getCalendarIdForUser(userId);
        TimeSlotEntity slot = timeSlotRepository
                .findByIdAndCalendarId(slotId, calendarId)
                .orElseThrow(() -> new EntityNotFoundException("slot not found : " + slotId));
        if (slot.isLinkedToMeeting()) {
            throw new SlotLinkedToMeetingException(slotId);
        }

        Instant newStart = request.startAt() != null ? request.startAt() : slot.getStartAt();
        Instant newEnd = request.endAt() != null ? request.endAt() : slot.getEndAt();
        requireValidRange(newStart, newEnd);
        slot.setStartAt(newStart);
        slot.setEndAt(newEnd);
        if (request.status() != null) {
            slot.setStatus(request.status());
        }

        TimeSlotEntity saved = timeSlotRepository.save(slot);
        return SlotResponse.from(saved, userId);
    }


    @Transactional
    public void deleteSlot(UUID slotId) {
        TimeSlotEntity slot = timeSlotRepository
                .findById(slotId)
                .orElseThrow(() -> new EntityNotFoundException("Time slot not found: " + slotId));
        if (slot.isLinkedToMeeting()) {
            throw new SlotLinkedToMeetingException(slotId);
        }
        timeSlotRepository.delete(slot);
    }


    private static void requireValidRange(Instant start, Instant end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("'endAt' must be after 'startAt'");
        }
    }


    @Transactional(readOnly = true)
    public Page<SlotResponse> listSlots(UUID userId, Instant from, Instant to, SlotStatus status, Pageable pageable) {
        UUID calendarId = calendarService.getCalendarIdForUser(userId);
        Page<TimeSlotEntity> page = status == null
                ? timeSlotRepository.findOverlapping(calendarId, from, to, pageable)
                : timeSlotRepository.findOverlappingByStatus(calendarId, status, from, to, pageable);
        return page.map(slot -> SlotResponse.from(slot, userId));
    }

}
