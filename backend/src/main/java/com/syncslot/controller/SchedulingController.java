package com.syncslot.controller;

import com.syncslot.dto.*;
import com.syncslot.service.CascadeService;
import com.syncslot.service.ConflictService;
import com.syncslot.service.SchedulingService;
import org.springframework.web.bind.annotation.*;

/**
 * Smart scheduling endpoints. All scheduling decisions live here (Java), the
 * frontend only calls these and renders the JSON results.
 */
@RestController
@RequestMapping("/api/schedule")
public class SchedulingController {

    private final SchedulingService schedulingService;
    private final ConflictService conflictService;
    private final CascadeService cascadeService;

    public SchedulingController(SchedulingService schedulingService,
                                ConflictService conflictService,
                                CascadeService cascadeService) {
        this.schedulingService = schedulingService;
        this.conflictService = conflictService;
        this.cascadeService = cascadeService;
    }

    @PostMapping("/find-slots")
    public FindSlotsResponse findSlots(@RequestBody FindSlotsRequest request) {
        return schedulingService.findSlots(request);
    }

    @PostMapping("/check-conflict")
    public ConflictResponse checkConflict(@RequestBody AppointmentRequest request) {
        return conflictService.detectConflicts(request);
    }

    @PostMapping("/preview-cascade")
    public CascadePreviewDto previewCascade(@RequestBody AppointmentRequest request) {
        return cascadeService.previewCascade(request);
    }

    @PostMapping("/apply-cascade")
    public CascadeApplyResponse applyCascade(@RequestBody CascadeApplyRequest request) {
        return cascadeService.applyCascade(request);
    }
}
