package stockmarket.datasource;

import stockmarket.model.Quote;
import stockmarket.model.TradeData;
import java.util.ArrayList;

/**
 * Abstract base class for data source implementations.
 */
public abstract class DataSourceBase {
  public ArrayList<TradeData> data;

  /**
   * Establishes connection to the data source.
   */
  public abstract void connect() throws InterruptedException;

  /**
   * Retrieves the list of available quotes/assets.
   */
  public abstract ArrayList<Quote> getQuotesList() throws Exception;

  /**
   * Retrieves the list of available intervals.
   */
  public abstract ArrayList<String> getIntervalList() throws Exception;
}
