package com.example.scheduler;

import java.util.UUID;

public class ParticipantUnavailableException extends RuntimeException {

    public ParticipantUnavailableException(UUID userId) {
        super("Participant is not available for the requested time: " + userId);
    }
}
