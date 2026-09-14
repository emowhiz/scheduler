package com.example.scheduler.user;

import com.example.scheduler.slot.domain.CreateSlotRequest;
import com.example.scheduler.slot.repository.TimeSlotRepository;
import com.example.scheduler.user.repository.CalendarEntity;
import com.example.scheduler.user.repository.CalendarRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalendarService {
    private final CalendarRepository calendarRepository;
    public UUID getCalendarIdForUser(UUID userId) {
        return calendarRepository.findByUserId(userId)
                .map(CalendarEntity::getId)
                .orElseThrow(() -> new EntityNotFoundException("user not found :" + userId));
    }
}
