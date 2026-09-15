package com.example.scheduler.meeting;

import java.util.UUID;

public class MeetingCancelledException extends RuntimeException {

    public MeetingCancelledException(UUID meetingId) {
        super("Meeting is cancelled and can no longer be modified: " + meetingId);
    }
}
