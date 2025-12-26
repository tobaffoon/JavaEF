package stockmarket.indicators;

import java.util.List;

public final class SMAIndicator extends Indicator {
    private final int period;

    public SMAIndicator(int period) {
        if (period <= 0) {
            throw new IllegalArgumentException("Period must be positive");
        }
        this.period = period;
    }

    @Override
    public double compute(List<Double> values, int index) {
        requireIndex(values, index);

        double sum = 0.0;
        for (int i = index - period + 1; i <= index; i++) {
            sum += values.get(i);
        }
        return sum / period;
    }

    @Override
    public int warmupPeriod() {
        return period;
    }
}
