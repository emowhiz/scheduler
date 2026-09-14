package com.example.scheduler.slot;

import java.util.UUID;

public class SlotLinkedToMeetingException extends RuntimeException {
    public SlotLinkedToMeetingException(UUID slotId) {
        super("slot already linked to an meeting and cannot be modified directly: "+slotId);
    }
}
