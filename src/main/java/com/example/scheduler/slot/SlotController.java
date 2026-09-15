package com.example.scheduler.slot;

import com.example.scheduler.common.PageResponse;
import com.example.scheduler.slot.domain.CreateSlotRequest;
import com.example.scheduler.slot.domain.SlotResponse;
import com.example.scheduler.slot.domain.UpdateSlotRequest;
import com.example.scheduler.slot.repository.SlotStatus;
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
@RequestMapping("api/v1/slots")
@RequiredArgsConstructor
public class SlotController {
    private final SlotService slotService;

    @PostMapping
    public ResponseEntity<SlotResponse> createSlot(@Valid @RequestBody CreateSlotRequest createSlotRequest) {
        SlotResponse response = slotService.createSlot(createSlotRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{slotId}")
    public SlotResponse updateSlot(@PathVariable UUID slotId,
                                   @RequestBody UpdateSlotRequest request) {
        return slotService.updateSlot(slotId, request);
    }
    @DeleteMapping("/{slotId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSlot(@PathVariable UUID slotId) {
        slotService.deleteSlot(slotId);
    }

    @GetMapping("/user/{userId}")
    public PageResponse<SlotResponse> listSlots(
            @PathVariable UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) SlotStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(slotService.listSlots(userId, from, to, status, pageable));
    }
}
