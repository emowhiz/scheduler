package com.example.scheduler.slot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeSlotRepository extends JpaRepository<TimeSlotEntity, UUID> {
    Optional<TimeSlotEntity> findByIdAndCalendarId(UUID id, UUID calendarId);

    Optional<TimeSlotEntity> findByMeetingIdAndCalendarId(UUID meetingId, UUID calendarId);
    List<TimeSlotEntity> findByMeetingId(UUID meetingId);

    @Query("""
            select case when count(s) > 0 then true else false end
            from TimeSlotEntity s
            where s.calendarId = :calendarId
              and s.status =  com.example.scheduler.slot.repository.SlotStatus.BUSY
              and s.startAt < :to and s.endAt > :from
            """)
    boolean existsOverlappingBusySlot(@Param("calendarId") UUID calendarId,
                                      @Param("from") Instant from,
                                      @Param("to") Instant to);

}
