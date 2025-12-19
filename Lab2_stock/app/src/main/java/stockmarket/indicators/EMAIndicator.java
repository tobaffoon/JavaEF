package stockmarket.indicators;

import java.util.ArrayList;

/**
 * Exponential Moving Average (EMA) indicator implementation.
 * Reference: https://pro-ts.ru/indikatory-foreks/1555-indikator-ema
 */
public class EMAIndicator extends IndicatorBase {
  /**
   * Calculates the Exponential Moving Average for forex data.
   *
   * @param close the list of closing prices
   * @param pricePercent the weight percentage for current price
   * @param beginTime the beginning index
   * @param time the current time index
   * @return the calculated EMA value
   */
  public static double calculateEma(ArrayList<Double> close, double pricePercent,
      int beginTime, int time) {
    if (time > beginTime + 1 && time < close.size()) {
      return close.get(time) * pricePercent + (calculateEma(close, pricePercent,
          beginTime, time - 1) * (1 - pricePercent));
    } else {
      return 0;
    }
  }
}
