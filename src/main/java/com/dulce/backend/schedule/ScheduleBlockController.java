package com.dulce.backend.schedule;

import com.dulce.backend.schedule.dto.ScheduleBlockCreateRequest;
import com.dulce.backend.schedule.dto.ScheduleBlockResponse;
import com.dulce.backend.schedule.dto.ScheduleBlockUpdateRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedule/blocks")
public class ScheduleBlockController {

    private final ScheduleBlockService scheduleBlockService;
    private final ScheduleBlockChecker scheduleBlockChecker;

    public ScheduleBlockController(
            ScheduleBlockService scheduleBlockService, ScheduleBlockChecker scheduleBlockChecker) {
        this.scheduleBlockService = scheduleBlockService;
        this.scheduleBlockChecker = scheduleBlockChecker;
    }

    @GetMapping
    public List<ScheduleBlockResponse> list(@RequestParam(required = false) Boolean active) {
        return scheduleBlockService.list(active);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/fully-blocked-days")
    public List<LocalDate> fullyBlockedDays(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return scheduleBlockChecker.findFullyBlockedDays(from, to);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ScheduleBlockResponse> create(
            @Valid @RequestBody ScheduleBlockCreateRequest request) {
        ScheduleBlockResponse response = scheduleBlockService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ScheduleBlockResponse update(
            @PathVariable Long id, @Valid @RequestBody ScheduleBlockUpdateRequest request) {
        return scheduleBlockService.update(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduleBlockService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
