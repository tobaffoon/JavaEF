package stockmarket.indicators;

import java.util.ArrayList;

/**
 * Simple Moving Average (SMA) indicator implementation.
 */
public class SMAIndicator extends IndicatorBase {
  /**
   * Calculates the Simple Moving Average.
   *
   * @param data the list of price data
   * @param begin the starting index
   * @param step the number of periods
   * @return the calculated SMA value
   */
  public static double calculateSma(ArrayList<Double> data, int begin, int step) {
    Double result = 0.0;
    int i = begin;
    for (; i < begin + step && i < data.size(); ++i) {
      result += data.get(i);
    }
    result /= (i - begin);
    return result;
  }
}
