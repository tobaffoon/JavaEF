package stockmarket.datasource;

import stockmarket.model.TradeData;
import java.util.ArrayList;

/**
 * Abstract base class for data source implementations.
 */
public abstract class DataSourceBase {
  protected String initMarket;
  protected String initQuote;
  protected String initContract;
  protected String initInterval;
  public ArrayList<TradeData> data;

  /**
   * Establishes connection to the data source.
   *
   * @throws InterruptedException if connection is interrupted
   */
  public abstract void connect() throws InterruptedException;

  /**
   * Initializes data source elements.
   *
   * @throws Exception if initialization fails
   */
  public abstract void initElements() throws Exception;

  /**
   * Retrieves the list of available markets.
   *
   * @return list of market names
   * @throws Exception if retrieval fails
   */
  public abstract ArrayList<String> getMarketList() throws Exception;

  /**
   * Retrieves the list of available quotes/assets.
   *
   * @return list of quote names
   * @throws Exception if retrieval fails
   */
  public abstract ArrayList<String> getQuotesList() throws Exception;

  /**
   * Retrieves the list of available intervals.
   *
   * @return list of interval names
   * @throws Exception if retrieval fails
   */
  public abstract ArrayList<String> getIntervalList() throws Exception;

  /**
   * Sets the selected market.
   *
   * @param marketName the market name
   * @param marketNumber the market number
   * @param marketPos the market position
   * @throws Exception if setting fails
   */
  public abstract void setMarket(String marketName, int marketNumber,
      int marketPos) throws Exception;

  /**
   * Sets the selected quote/asset.
   *
   * @param quoteName the quote name
   * @param quoteNumber the quote number
   * @param quotePos the quote position
   * @throws Exception if setting fails
   */
  public abstract void setQuote(String quoteName, int quoteNumber, int quotePos)
      throws Exception;

  /**
   * Sets the selected interval.
   *
   * @param intervalName the interval name
   * @param intervalNumber the interval number
   * @throws Exception if setting fails
   */
  public abstract void setInterval(String intervalName, int intervalNumber)
      throws Exception;

  /**
   * Sets the beginning date to current date minus one year.
   */
  public abstract void setBeginDate();

  /**
   * Sets the ending date to current date.
   */
  public abstract void setEndDate();

  /**
   * Gets the minimum available date.
   *
   * @return the minimum date as string
   */
  public abstract String getMinDate();

  /**
   * Fetches market data for the selected parameters.
   *
   * @throws Exception if data retrieval fails
   */
  public abstract void getData() throws Exception;

  /**
   * Sets the beginning date with specific day, month, and year.
   *
   * @param day the day
   * @param month the month
   * @param year the year
   */
  public abstract void setBeginData(int day, int month, int year);

  /**
   * Sets the ending date with specific day, month, and year.
   *
   * @param day the day
   * @param month the month
   * @param year the year
   */
  public abstract void setEndData(int day, int month, int year);
}
