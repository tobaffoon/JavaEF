package creditpay.model;

import java.time.LocalDate;

/**
 * Период как диапазон по числам месяца (например, с 26 по 25).
 */
public final class InterestPeriod {
    private final int paymentDay;

    public InterestPeriod(int paymentDay) {
        if (paymentDay < 1 || paymentDay > 31) {
            throw new IllegalArgumentException("paymentDay must be 1..31");
        }
        this.paymentDay = paymentDay;
    }

    public int getPaymentDay() {
        return paymentDay;
    }

    public LocalDate nextAccrualDate(LocalDate previousAccrualDate) {
        if (previousAccrualDate == null) {
            throw new IllegalArgumentException("previousAccrualDate must not be null");
        }
        LocalDate candidate = previousAccrualDate.plusMonths(1);
        return candidate;
    }
}
