package com.example.scheduler.slot;

import com.example.scheduler.slot.domain.CreateSlotRequest;
import com.example.scheduler.slot.domain.SlotResponse;
import com.example.scheduler.slot.domain.UpdateSlotRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
