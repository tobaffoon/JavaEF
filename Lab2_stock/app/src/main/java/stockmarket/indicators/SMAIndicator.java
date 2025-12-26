package stockmarket.indicators;

import java.util.List;

/**
 * Simple Moving Average (SMA) indicator implementation.
 */
public class SMAIndicator extends IndicatorBase {
  /**
   * Calculates the Simple Moving Average.
   */
  public static double calculateSma(List<Double> data, int begin, int step) {
    Double result = 0.0;
    int i = begin;
    for (; i < begin + step && i < data.size(); ++i) {
      result += data.get(i);
    }
    result /= (i - begin);
    return result;
  }
}
