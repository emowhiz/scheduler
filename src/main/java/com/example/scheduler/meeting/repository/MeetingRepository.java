package com.example.scheduler.meeting.repository;

import com.example.scheduler.user.repository.CalendarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<MeetingEntity, UUID> {
    @Query("""
        select u.id from TimeSlotEntity t
        join CalendarEntity c on c.id = t.calendarId
        join UserEntity u on u.id = c.userId
        join MeetingEntity m on m.id = t.meetingId
        where t.meetingId = :meetingId
          and u.id <> m.organizerUserId
        """)
    List<UUID> findAllParticipantsForMeetingId(@Param("meetingId") UUID meetingId);
}
