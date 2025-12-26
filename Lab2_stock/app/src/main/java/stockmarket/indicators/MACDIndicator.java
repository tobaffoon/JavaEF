package stockmarket.indicators;

import java.util.List;

public final class MACDIndicator extends Indicator {

    private final EMAIndicator fastEma;
    private final EMAIndicator slowEma;

    public MACDIndicator(int fastPeriod, int slowPeriod) {
        if (fastPeriod >= slowPeriod) {
            throw new IllegalArgumentException(
                "Fast period must be smaller than slow period"
            );
        }
        this.fastEma = new EMAIndicator(fastPeriod);
        this.slowEma = new EMAIndicator(slowPeriod);
    }

    @Override
    public double compute(List<Double> values, int index) {
        requireIndex(values, index);

        double fast = fastEma.compute(values, index);
        double slow = slowEma.compute(values, index);
        return fast - slow;
    }

    @Override
    public int warmupPeriod() {
        return slowEma.warmupPeriod();
    }
}
