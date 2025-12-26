package stockmarket.indicators;

import java.util.List;

public abstract class Indicator {

    /**
     * Computes indicator value at a given index.
     */
    public abstract double compute(List<Double> values, int index);

    /**
     * Minimum number of data points required.
     */
    public abstract int warmupPeriod();

    protected void requireIndex(List<Double> values, int index) {
        if (index < warmupPeriod() - 1 || index >= values.size()) {
            throw new IllegalArgumentException(
                "Index out of bounds for indicator computation"
            );
        }
    }
}
