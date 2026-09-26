package com.dulce.backend.schedule.dto;

import com.dulce.backend.schedule.ScheduleBlockType;
import com.dulce.backend.schedule.ScheduleRecurrence;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleBlockCreateRequest(
        @NotNull ScheduleBlockType type,
        LocalDate blockDate,
        ScheduleRecurrence recurrence,
        Short weekday,
        Short monthDay,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        LocalDate validFrom,
        LocalDate validUntil) {}
