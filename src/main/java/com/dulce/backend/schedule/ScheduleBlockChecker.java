package com.dulce.backend.schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Service;

@Service
public class ScheduleBlockChecker {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    private final ScheduleBlockRepository scheduleBlockRepository;

    public ScheduleBlockChecker(ScheduleBlockRepository scheduleBlockRepository) {
        this.scheduleBlockRepository = scheduleBlockRepository;
    }

    public boolean isBlocked(OffsetDateTime pickupAt) {
        ZonedDateTime local = pickupAt.atZoneSameInstant(BUSINESS_ZONE);
        LocalDate date = local.toLocalDate();
        LocalTime time = local.toLocalTime();

        return scheduleBlockRepository.findByActiveTrue().stream()
                .anyMatch(block -> matches(block, date, time));
    }

    private boolean matches(ScheduleBlock block, LocalDate date, LocalTime time) {
        if (!withinTimeWindow(block, time) || !withinValidityWindow(block, date)) {
            return false;
        }

        return switch (block.getType()) {
            case EVENTUAL -> date.equals(block.getBlockDate());
            case RECURRING -> matchesRecurrence(block, date);
        };
    }

    private boolean matchesRecurrence(ScheduleBlock block, LocalDate date) {
        if (block.getRecurrence() == null) {
            return false;
        }

        return switch (block.getRecurrence()) {
            case DAILY -> true;
            case WEEKLY ->
                    block.getWeekday() != null
                            && block.getWeekday() == date.getDayOfWeek().getValue();
            case MONTHLY ->
                    block.getMonthDay() != null && block.getMonthDay() == date.getDayOfMonth();
        };
    }

    private boolean withinTimeWindow(ScheduleBlock block, LocalTime time) {
        return !time.isBefore(block.getStartTime()) && time.isBefore(block.getEndTime());
    }

    private boolean withinValidityWindow(ScheduleBlock block, LocalDate date) {
        if (date.isBefore(block.getValidFrom())) {
            return false;
        }

        return block.getValidUntil() == null || !date.isAfter(block.getValidUntil());
    }
}
