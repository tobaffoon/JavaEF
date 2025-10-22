package creditpay.model;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Период как диапазон по числам месяца (например, с 26 по 25).
 */
public final class DayOfMonthRangePeriod implements InterestPeriod {
    private final int startDay; // 1..31
    private final int endDay; // 1..31

    public DayOfMonthRangePeriod(int startDay, int endDay) {
        if (startDay < 1 || startDay > 31) throw new IllegalArgumentException("startDay must be 1..31");
        if (endDay < 1 || endDay > 31) throw new IllegalArgumentException("endDay must be 1..31");
        this.startDay = startDay;
        this.endDay = endDay;
    }

    public int getStartDay() { return startDay; }
    public int getEndDay() { return endDay; }

    @Override
    public LocalDate nextAccrualDate(LocalDate previousAccrualDate) {
        if (previousAccrualDate == null) throw new IllegalArgumentException("previousAccrualDate must not be null");
        LocalDate candidate = previousAccrualDate.plusMonths(1);
        YearMonth ym = YearMonth.from(candidate);
        int day = Math.min(endDay, ym.lengthOfMonth());
        return ym.atDay(day);
    }

    @Override
    public String toString() {
        return String.format("%d по %d число месяца", startDay, endDay);
    }
}
