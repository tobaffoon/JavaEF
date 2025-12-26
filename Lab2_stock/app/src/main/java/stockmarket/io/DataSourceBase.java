package stockmarket.io;

import stockmarket.model.Quote;
import java.util.ArrayList;

public interface DataSourceBase {
  void connect() throws Exception;

  ArrayList<Quote> getQuotesList() throws Exception;
}
