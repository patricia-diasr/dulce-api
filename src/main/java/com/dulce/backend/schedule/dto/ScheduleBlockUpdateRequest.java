package com.dulce.backend.schedule.dto;

import com.dulce.backend.schedule.ScheduleBlockType;
import com.dulce.backend.schedule.ScheduleRecurrence;
import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleBlockUpdateRequest(
        ScheduleBlockType type,
        LocalDate blockDate,
        ScheduleRecurrence recurrence,
        Short weekday,
        Short monthDay,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate validFrom,
        LocalDate validUntil) {}
