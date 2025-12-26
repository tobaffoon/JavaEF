package stockmarket.indicators;

import java.util.List;

public final class EMAIndicator extends Indicator {
    private int period;
    private double alpha;

    public EMAIndicator() {
    }

    public void setPeriod(int period) {
        if (period <= 0) {
            throw new IllegalArgumentException("Period must be positive");
        }
        this.period = period;
        this.alpha = 2.0 / (period + 1);
    }

    @Override
    public double compute(List<Double> values, int index) {
        requireIndex(values, index);

        double ema = values.get(index - period + 1);
        for (int i = index - period + 2; i <= index; i++) {
            ema = alpha * values.get(i) + (1 - alpha) * ema;
        }
        return ema;
    }

    @Override
    public int warmupPeriod() {
        return period;
    }

    @Override
    public String toString() {
        return "EMA";
    }
}
