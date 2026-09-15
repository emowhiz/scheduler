package com.example.scheduler.meeting;

import com.example.scheduler.ParticipantUnavailableException;
import com.example.scheduler.meeting.domain.CreateMeetingRequest;
import com.example.scheduler.meeting.domain.MeetingResponse;
import com.example.scheduler.meeting.domain.UpdateMeetingRequest;
import com.example.scheduler.meeting.repository.MeetingEntity;
import com.example.scheduler.meeting.repository.MeetingRepository;
import com.example.scheduler.meeting.repository.MeetingStatus;
import com.example.scheduler.slot.SlotLinkedToMeetingException;
import com.example.scheduler.slot.SlotNotFreeException;
import com.example.scheduler.slot.repository.SlotStatus;
import com.example.scheduler.slot.repository.TimeSlotEntity;
import com.example.scheduler.slot.repository.TimeSlotRepository;
import com.example.scheduler.user.CalendarService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MeetingService {
    private final MeetingRepository meetingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final CalendarService calendarService;
    private final MeterRegistry meterRegistry;

    @Transactional(readOnly = true)
    public MeetingResponse getMeeting(UUID meetingId) {
        MeetingEntity meeting = requireMeeting(meetingId);
        return MeetingResponse.from(meeting, meetingRepository.findAllParticipantsForMeetingId(meetingId));
    }

    @Transactional
    public void cancelMeeting(UUID meetingId) {
        MeetingEntity meeting = requireMeeting(meetingId);
        if (meeting.isCancelled()) {
            return;
        }
        meeting.setStatus(MeetingStatus.CANCELLED);
        meetingRepository.save(meeting);
        meterRegistry.counter("meetings.cancelled").increment();

        UUID organizerCalendarId = calendarService.getCalendarIdForUser(meeting.getOrganizerUserId());
        List<TimeSlotEntity> linkedSlots = timeSlotRepository.findByMeetingId(meetingId);
        for (TimeSlotEntity slot : linkedSlots) {
            if (slot.getCalendarId().equals(organizerCalendarId)) {
                slot.setStatus(SlotStatus.FREE);
                slot.setMeetingId(null);
                timeSlotRepository.save(slot);
            } else {
                timeSlotRepository.delete(slot);
            }
        }
    }

    @Transactional
    public MeetingResponse updateMeeting(UUID meetingId, UpdateMeetingRequest request) {
        MeetingEntity meeting = requireMeeting(meetingId);
        if (meeting.isCancelled()) {
            throw new MeetingCancelledException(meetingId);
        }
        if (request.title() != null) {
            meeting.setTitle(request.title());
        }
        if (request.description() != null) {
            meeting.setDescription(request.description());
        }

        if (request.participantUserIds() != null) {
            reconcileParticipants(meeting, request.participantUserIds());
        }

        MeetingEntity saved = meetingRepository.saveAndFlush(meeting);
        return MeetingResponse.from(saved, meetingRepository.findAllParticipantsForMeetingId(meetingId));
    }

    private void reconcileParticipants(MeetingEntity meeting, List<UUID> requestedParticipantIds) {
        Set<UUID> requested = new HashSet<>(requestedParticipantIds);
        requested.remove(meeting.getOrganizerUserId());

        Set<UUID> currentIds = new HashSet<>(meetingRepository.findAllParticipantsForMeetingId(meeting.getId()));

        Set<UUID> toRemove = new HashSet<>(currentIds);
        toRemove.removeAll(requested);
        Set<UUID> toAdd = new HashSet<>(requested);
        toAdd.removeAll(currentIds);

        for (UUID userId : toRemove) {
            removeParticipant(meeting, userId);
        }
        for (UUID userId : toAdd) {
            addParticipant(meeting, userId);
        }
    }

    private void removeParticipant(MeetingEntity meeting, UUID userId) {
        UUID calendarId = calendarService.getCalendarIdForUser(userId);
        timeSlotRepository
                .findByMeetingIdAndCalendarId(meeting.getId(), calendarId)
                .ifPresent(timeSlotRepository::delete);
    }

    private void addParticipant(MeetingEntity meeting, UUID userId) {
        UUID calendarId = calendarService.getCalendarIdForUser(userId);
        boolean busy = timeSlotRepository.existsOverlappingBusySlot(
                calendarId, meeting.getStartAt(), meeting.getEndAt());
        if (busy) {
            meterRegistry.counter("booking.conflicts").increment();
            throw new ParticipantUnavailableException(userId);
        }
        TimeSlotEntity mirrored = new TimeSlotEntity(calendarId, meeting.getStartAt(), meeting.getEndAt());
        mirrored.setStatus(SlotStatus.BUSY);
        mirrored.setMeetingId(meeting.getId());
        timeSlotRepository.save(mirrored);
    }

    @Transactional
    public MeetingResponse createMeeting(UUID organizerId, UUID slotId, @Valid CreateMeetingRequest request) {
        TimeSlotEntity slotForUser = getAvailableSlotForUser(organizerId, slotId);
        List<UUID> participantIds = checkParticipantSlots(organizerId, request, slotForUser);
        MeetingEntity meeting = saveMeetingEntity(organizerId, request, slotForUser);
        updateSlot(slotForUser, meeting);

        List<TimeSlotEntity> mirroredSlots = getMirroredSlots(participantIds, slotForUser, meeting);
        timeSlotRepository.saveAll(mirroredSlots);
        meterRegistry.counter("meetings.creation").increment();

        return MeetingResponse.from(meeting, participantIds);
    }

    private List<TimeSlotEntity> getMirroredSlots(List<UUID> participantIds, TimeSlotEntity slot, MeetingEntity meeting) {
        List<TimeSlotEntity> mirroredSlots = new ArrayList<>();
        for (UUID participantId : participantIds) {
            UUID participantCalendarId = calendarService.getCalendarIdForUser(participantId);
            TimeSlotEntity mirrored = new TimeSlotEntity(participantCalendarId, slot.getStartAt(), slot.getEndAt());
            mirrored.setStatus(SlotStatus.BUSY);
            mirrored.setMeetingId(meeting.getId());
            mirroredSlots.add(mirrored);
        }
        return mirroredSlots;
    }

    private List<UUID> checkParticipantSlots(UUID organizerId, CreateMeetingRequest request, TimeSlotEntity slotForUser) {
        List<UUID> participantIds = request.participantsOrEmpty().stream()
                .distinct()
                .filter(id -> !id.equals(organizerId))
                .toList();
        for (UUID participantId : participantIds) {
            boolean busy = isSlotBusy(participantId, slotForUser.getStartAt(), slotForUser.getEndAt());
            if (busy) {
                meterRegistry.counter("booking.conflicts").increment();
                throw new ParticipantUnavailableException(participantId);
            }
        }
        return participantIds;
    }

    private void updateSlot(TimeSlotEntity slotForUser, MeetingEntity meeting) {
        slotForUser.setStatus(SlotStatus.BUSY);
        slotForUser.setMeetingId(meeting.getId());
        timeSlotRepository.save(slotForUser);
    }

    private MeetingEntity saveMeetingEntity(UUID organizerId, CreateMeetingRequest request, TimeSlotEntity slotForUser) {
        MeetingEntity meetingEntity = new MeetingEntity(organizerId,
                request.title(),
                request.description(),
                slotForUser.getStartAt(), slotForUser.getEndAt());
        return meetingRepository.saveAndFlush(meetingEntity);
    }


    private TimeSlotEntity getAvailableSlotForUser(UUID userId, UUID slotId) {
        UUID calendarId = calendarService.getCalendarIdForUser(userId);
        TimeSlotEntity slot = timeSlotRepository
                .findByIdAndCalendarId(slotId, calendarId)
                .orElseThrow(() -> new EntityNotFoundException("slot not found : " + slotId));
        if (slot.isLinkedToMeeting()) {
            meterRegistry.counter("booking.conflicts").increment();
            throw new SlotLinkedToMeetingException(slotId);
        }
        if (!slot.isFree()) {
            meterRegistry.counter("booking.conflicts").increment();
            throw new SlotNotFreeException(slotId);
        }
        return slot;
    }

    private boolean isSlotBusy(UUID participantId, Instant start, Instant end) {
        UUID participantCalendarId = calendarService.getCalendarIdForUser(participantId);
        return timeSlotRepository.existsOverlappingBusySlot(participantCalendarId, start, end);
    }

    private MeetingEntity requireMeeting(UUID meetingId) {
        return meetingRepository.findById(meetingId).orElseThrow(() -> new EntityNotFoundException("Meeting not found : " + meetingId));
    }

}
