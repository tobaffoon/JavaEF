package stockmarket.io;

import stockmarket.model.Bar;
import stockmarket.model.Quote;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public interface DataSourceBase {
  void connect() throws Exception;

  ArrayList<Quote> getQuotesList() throws Exception;

  List<Bar> getBars(String symbol, LocalDateTime startTime, LocalDateTime endTime) throws Exception;
}
