package creditpay.model;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Период как диапазон по числам месяца (например, с 26 по 25).
 */
public final class DayOfMonthRangePeriod implements InterestPeriod {
    private final int paymentDay;

    public DayOfMonthRangePeriod(int paymentDay) {
        if (paymentDay < 1 || paymentDay > 31) {
            throw new IllegalArgumentException("paymentDay must be 1..31");
        }
        this.paymentDay = paymentDay;
    }

    public int getPaymentDay() {
        return paymentDay;
    }

    @Override
    public LocalDate nextAccrualDate(LocalDate previousAccrualDate) {
        if (previousAccrualDate == null) {
            throw new IllegalArgumentException("previousAccrualDate must not be null");
        }
        LocalDate candidate = previousAccrualDate.plusMonths(1);
        YearMonth ym = YearMonth.from(candidate);
        int day = Math.min(paymentDay, ym.lengthOfMonth());
        return ym.atDay(day);
    }
}
