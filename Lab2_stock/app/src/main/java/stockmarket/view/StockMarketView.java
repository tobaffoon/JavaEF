package stockmarket.view;

import stockmarket.io.DataSourceBase;
import stockmarket.model.Interval;
import stockmarket.model.Quote;

import java.awt.Color;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMarketView {

    void setConnectionStatus(String status, Color color);

    void setExecutionStatus(String status, Color color);

    void setError(Exception exception);

    void setDataSourceOptions(List<DataSourceBase> options);

    Quote getSelectedQuote();

    Interval getSelectedInterval();

    void setQuoteOptions(List<Quote> quotes);

    LocalDateTime getBeginDate();

    void setBeginDate(LocalDateTime date);

    LocalDateTime getEndDate();

    void setEndDate(LocalDateTime date);
}
