package com.example.scheduler.meeting;

import com.example.scheduler.common.PageResponse;
import com.example.scheduler.meeting.domain.CreateMeetingRequest;
import com.example.scheduler.meeting.domain.MeetingResponse;
import com.example.scheduler.meeting.domain.UpdateMeetingRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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

    @GetMapping("/users/{userId}")
    public PageResponse<MeetingResponse> listMeetingsForUser(
            @PathVariable UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(meetingService.listMeetingsForUser(userId, from, to, pageable));
    }
}
