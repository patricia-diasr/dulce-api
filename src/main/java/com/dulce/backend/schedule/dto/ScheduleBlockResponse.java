package com.dulce.backend.schedule.dto;

import com.dulce.backend.schedule.ScheduleBlockType;
import com.dulce.backend.schedule.ScheduleRecurrence;
import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleBlockResponse(
        Long id,
        ScheduleBlockType type,
        LocalDate blockDate,
        Short weekday,
        Short monthDay,
        ScheduleRecurrence recurrence,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate validFrom,
        LocalDate validUntil,
        boolean active) {}
