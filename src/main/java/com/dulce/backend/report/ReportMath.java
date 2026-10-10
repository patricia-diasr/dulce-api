package com.dulce.backend.report;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class ReportMath {

    static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private ReportMath() {}

    static BigDecimal money(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal average(BigDecimal total, long count) {
        return count == 0 ? ZERO : total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }
}
