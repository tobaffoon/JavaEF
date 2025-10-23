package creditpay.model;

import java.time.LocalDate;

/**
 * Фиксированная длина периода в днях.
 */
public final class FixedLengthPeriod implements InterestPeriod {
    private final int lengthInDays;

    public FixedLengthPeriod(int lengthInDays) {
        if (lengthInDays <= 0) throw new IllegalArgumentException("lengthInDays must be > 0");
        this.lengthInDays = lengthInDays;
    }

    public int getLengthInDays() { return lengthInDays; }

    @Override
    public LocalDate nextAccrualDate(LocalDate previousAccrualDate) {
        if (previousAccrualDate == null) throw new IllegalArgumentException("previousAccrualDate must not be null");
        return previousAccrualDate.plusDays(lengthInDays);
    }
}
