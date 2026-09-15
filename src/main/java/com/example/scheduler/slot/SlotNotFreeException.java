package com.example.scheduler.slot;

import java.util.UUID;

public class SlotNotFreeException extends RuntimeException {

    public SlotNotFreeException(UUID slotId) {
        super("Time slot is not free: " + slotId);
    }
}
