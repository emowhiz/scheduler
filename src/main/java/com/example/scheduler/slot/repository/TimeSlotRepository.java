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
            select case when count(t) > 0 then true else false end
            from TimeSlotEntity t
            where t.calendarId = :calendarId
              and t.status =  com.example.scheduler.slot.repository.SlotStatus.BUSY
              and t.startAt < :to and t.endAt > :from
            """)
    boolean existsOverlappingBusySlot(@Param("calendarId") UUID calendarId,
                                      @Param("from") Instant from,
                                      @Param("to") Instant to);

    @Query("""
            select t from TimeSlotEntity t
            where t.calendarId in :calendarIds
              and t.startAt < :to and t.endAt > :from
            order by t.calendarId asc, t.startAt asc
            """)
    List<TimeSlotEntity> findAllOverlappingForCalendars(
            @Param("calendarIds") List<UUID> calendarIds,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
