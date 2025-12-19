package stockmarket.indicators;

import java.util.ArrayList;

/**
 * MACD (Moving Average Convergence Divergence) indicator implementation.
 * Reference: https://pro-ts.ru/indikatory-foreks/63-indikator-macd
 * Typically uses periods 12 and 26.
 */
public class MACDIndicator extends IndicatorBase {
  /**
   * Calculates the MACD indicator with specified fast and slow EMA widths.
   *
   * @param data the list of price data
   * @param begin the starting index
   * @param fastEmaWidth the fast EMA period (usually 12)
   * @param slowEmaWidth the slow EMA period (usually 26)
   * @return the calculated MACD value
   */
  public static double calculateMacd(ArrayList<Double> data, int begin,
      int fastEmaWidth, int slowEmaWidth) {
    var macdFastEma = EMAIndicator.calculateEma(data,
        2.0 / (1 + fastEmaWidth - begin), begin, fastEmaWidth);
    var macdSlowEma = EMAIndicator.calculateEma(data,
        2.0 / (1 + slowEmaWidth - begin), begin, slowEmaWidth);
    return macdFastEma - macdSlowEma;
  }

  /**
   * Calculates the MACD indicator with default periods (12, 26).
   *
   * @param data the list of price data
   * @param begin the starting index
   * @return the calculated MACD value
   */
  public static double calculateMacd(ArrayList<Double> data, int begin) {
    return calculateMacd(data, begin, 12, 26);
  }
}
