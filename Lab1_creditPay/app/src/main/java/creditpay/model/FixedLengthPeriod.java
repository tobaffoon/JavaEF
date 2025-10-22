package creditpay.model;

import java.time.LocalDate;

/**
 * Фиксированная длина периода в днях, с необязательным смещением старта.
 */
public final class FixedLengthPeriod implements InterestPeriod {
    private final int lengthInDays;
    private final int offsetDays;

    public FixedLengthPeriod(int lengthInDays, int offsetDays) {
        if (lengthInDays <= 0) throw new IllegalArgumentException("lengthInDays must be > 0");
        this.lengthInDays = lengthInDays;
        this.offsetDays = offsetDays;
    }

    public int getLengthInDays() { return lengthInDays; }
    public int getOffsetDays() { return offsetDays; }

    @Override
    public LocalDate nextAccrualDate(LocalDate previousAccrualDate) {
        if (previousAccrualDate == null) throw new IllegalArgumentException("previousAccrualDate must not be null");
        return previousAccrualDate.plusDays(lengthInDays);
    }

    @Override
    public String toString() {
        if (offsetDays != 0) return String.format("%d дней, смещение %d дней", lengthInDays, offsetDays);
        return String.format("%d дней", lengthInDays);
    }
}
