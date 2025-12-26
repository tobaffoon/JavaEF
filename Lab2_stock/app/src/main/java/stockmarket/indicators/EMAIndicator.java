package stockmarket.indicators;

import java.util.List;

/**
 * Exponential Moving Average (EMA) indicator implementation.
 * Reference: https://pro-ts.ru/indikatory-foreks/1555-indikator-ema
 */
public class EMAIndicator extends IndicatorBase {
  /**
   * Calculates the Exponential Moving Average for forex data.
   */
  public static double calculateEma(List<Double> close, double pricePercent,
      int beginTime, int time) {
    if (time > beginTime + 1 && time < close.size()) {
      return close.get(time) * pricePercent + (calculateEma(close, pricePercent,
          beginTime, time - 1) * (1 - pricePercent));
    } else {
      return 0;
    }
  }
}
