package com.example.scheduler.slot.repository;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "time_slots")
@Getter
@Setter
@NoArgsConstructor
public class TimeSlotEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "calendar_id", nullable = false)
    private UUID calendarId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SlotStatus status = SlotStatus.FREE;

    @Column(name = "meeting_id")
    private UUID meetingId;

    @Version
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TimeSlotEntity(UUID calendarId, Instant startAt, Instant endAt) {
        this.calendarId = calendarId;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public boolean isLinkedToMeeting() {
        return meetingId != null;
    }
}
