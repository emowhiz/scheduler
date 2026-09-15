package com.example.scheduler.meeting;

import com.example.scheduler.meeting.domain.CreateMeetingRequest;
import com.example.scheduler.meeting.domain.MeetingResponse;
import com.example.scheduler.meeting.domain.UpdateMeetingRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/meetings")
@RequiredArgsConstructor
public class MeetingController {
    private final MeetingService meetingService;

    @PostMapping("/organizer/{organizerId}/slot/{slotId}")
    public ResponseEntity<MeetingResponse> createMeeting(@PathVariable UUID organizerId,
                                                         @PathVariable UUID slotId,
                                                         @Valid @RequestBody CreateMeetingRequest createMeetingRequest) {
        MeetingResponse response = meetingService.createMeeting(organizerId, slotId, createMeetingRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/{meetingId}")
    public MeetingResponse getMeeting(@PathVariable UUID meetingId) {
        return meetingService.getMeeting(meetingId);
    }

    @PatchMapping("/{meetingId}")
    public MeetingResponse updateMeeting(
            @PathVariable UUID meetingId, @RequestBody UpdateMeetingRequest request) {
        return meetingService.updateMeeting(meetingId, request);
    }

    @DeleteMapping("/{meetingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelMeeting(@PathVariable UUID meetingId) {
        meetingService.cancelMeeting(meetingId);
    }
}
