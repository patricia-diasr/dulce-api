package com.dulce.backend.schedule;

import com.dulce.backend.common.exception.BadRequestException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ScheduleBlockChecker {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final int SAMPLE_STEP_MINUTES = 30;
    private static final int MINUTES_PER_DAY = 24 * 60;
    private static final long MAX_RANGE_DAYS = 366;

    private final ScheduleBlockRepository scheduleBlockRepository;

    public ScheduleBlockChecker(ScheduleBlockRepository scheduleBlockRepository) {
        this.scheduleBlockRepository = scheduleBlockRepository;
    }

    public boolean isBlocked(OffsetDateTime pickupAt) {
        ZonedDateTime local = pickupAt.atZoneSameInstant(BUSINESS_ZONE);
        LocalDate date = local.toLocalDate();
        LocalTime time = local.toLocalTime();

        List<ScheduleBlock> activeBlocks = scheduleBlockRepository.findByActive(true);
        return activeBlocks.stream().anyMatch(block -> matches(block, date, time));
    }

    public List<LocalDate> findFullyBlockedDays(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BadRequestException("Data final não pode ser anterior à data inicial.");
        }

        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new BadRequestException("Intervalo máximo de consulta é de 366 dias.");
        }

        List<ScheduleBlock> activeBlocks = scheduleBlockRepository.findByActive(true);
        List<LocalDate> fullyBlockedDays = new ArrayList<>();

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (isDayFullyBlocked(activeBlocks, date)) {
                fullyBlockedDays.add(date);
            }
        }

        return fullyBlockedDays;
    }

    private boolean isDayFullyBlocked(List<ScheduleBlock> activeBlocks, LocalDate date) {
        for (int minutes = 0; minutes < MINUTES_PER_DAY; minutes += SAMPLE_STEP_MINUTES) {
            LocalTime time = LocalTime.MIDNIGHT.plusMinutes(minutes);
            boolean blocked = activeBlocks.stream().anyMatch(block -> matches(block, date, time));

            if (!blocked) {
                return false;
            }
        }

        return true;
    }

    private boolean matches(ScheduleBlock block, LocalDate date, LocalTime time) {
        if (!withinTimeWindow(block, time)) {
            return false;
        }

        LocalDate anchorDate =
                crossesMidnight(block) && time.isBefore(block.getEndTime())
                        ? date.minusDays(1)
                        : date;

        if (!withinValidityWindow(block, anchorDate)) {
            return false;
        }

        return switch (block.getType()) {
            case EVENTUAL -> anchorDate.equals(block.getBlockDate());
            case RECURRING -> matchesRecurrence(block, anchorDate);
        };
    }

    private boolean matchesRecurrence(ScheduleBlock block, LocalDate anchorDate) {
        if (block.getRecurrence() == null) {
            return false;
        }

        return switch (block.getRecurrence()) {
            case DAILY -> true;
            case WEEKLY ->
                    block.getWeekday() != null
                            && block.getWeekday() == anchorDate.getDayOfWeek().getValue();
            case MONTHLY ->
                    block.getMonthDay() != null
                            && block.getMonthDay() == anchorDate.getDayOfMonth();
        };
    }

    private boolean crossesMidnight(ScheduleBlock block) {
        return block.getStartTime().isAfter(block.getEndTime());
    }

    private boolean withinTimeWindow(ScheduleBlock block, LocalTime time) {
        LocalTime start = block.getStartTime();
        LocalTime end = block.getEndTime();

        if (!crossesMidnight(block)) {
            return !time.isBefore(start) && time.isBefore(end);
        }

        return !time.isBefore(start) || time.isBefore(end);
    }

    private boolean withinValidityWindow(ScheduleBlock block, LocalDate anchorDate) {
        if (anchorDate.isBefore(block.getValidFrom())) {
            return false;
        }

        return block.getValidUntil() == null || !anchorDate.isAfter(block.getValidUntil());
    }
}
