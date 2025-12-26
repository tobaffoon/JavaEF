package stockmarket.indicators;

import java.util.List;

public final class MACDIndicator extends Indicator {
    private EMAIndicator fastEma;
    private EMAIndicator slowEma;

    public MACDIndicator() {
    }

    public void setPeriods(int fastPeriod, int slowPeriod) {
        if (fastPeriod >= slowPeriod) {
            throw new IllegalArgumentException(
                "Fast period must be smaller than slow period"
            );
        }
        this.fastEma = new EMAIndicator();
        this.fastEma.setPeriod(fastPeriod);
        this.slowEma = new EMAIndicator();
        this.slowEma.setPeriod(slowPeriod);
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

    @Override
    public String toString() {
        return "MACD";
    }
}
