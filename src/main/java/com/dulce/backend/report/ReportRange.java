package com.dulce.backend.report;

import com.dulce.backend.common.exception.BadRequestException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public record ReportRange(LocalDate from, LocalDate to) {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    public static final long DEFAULT_MAX_DAYS = 1830;
    private static final int DEFAULT_PERIOD_DAYS = 30;

    public static ReportRange resolve(LocalDate from, LocalDate to, long maxDays) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        return resolve(
                from,
                to,
                maxDays,
                new ReportRange(today.minusDays(DEFAULT_PERIOD_DAYS - 1), today));
    }

    public static ReportRange resolve(
            LocalDate from, LocalDate to, long maxDays, ReportRange defaultRange) {
        if ((from == null) != (to == null)) {
            throw new BadRequestException("Informe as duas datas do período, ou nenhuma.");
        }

        if (from == null) {
            return defaultRange;
        }

        if (to.isBefore(from)) {
            throw new BadRequestException("Data final não pode ser anterior à data inicial.");
        }

        if (ChronoUnit.DAYS.between(from, to) + 1 > maxDays) {
            throw new BadRequestException(
                    "Intervalo máximo de consulta é de " + maxDays + " dias.");
        }

        return new ReportRange(from, to);
    }

    public OffsetDateTime start() {
        return from.atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
    }

    public OffsetDateTime endExclusive() {
        return to.plusDays(1).atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
    }

    public static OffsetDateTime toBusinessZone(OffsetDateTime value) {
        return value == null ? null : value.atZoneSameInstant(BUSINESS_ZONE).toOffsetDateTime();
    }
}
