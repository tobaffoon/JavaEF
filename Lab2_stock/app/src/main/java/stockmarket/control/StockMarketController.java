package stockmarket.control;

import stockmarket.io.DataSourceBase;
import stockmarket.io.FinamApiClient;
import stockmarket.model.Bar;
import stockmarket.model.Interval;
import stockmarket.model.Quote;
import stockmarket.view.StockMarketView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StockMarketController {
    private static final int SCALE = 10;

    private StockMarketView view;
    private ArrayList<DataSourceBase> dataSourceList;
    private DataSourceBase selectedDataSource;
    private List<Quote> quoteList;

    public StockMarketController(StockMarketView view) {
        this.view = view;
        initDataSourceList();
    }

    public List<Quote> getQuoteList(){
        return quoteList;
    }
    
    public ArrayList<DataSourceBase> getDataSourceList(){
        return dataSourceList;
    }

    public List<Bar> getBars(String symbol, Interval interval,
        java.time.LocalDateTime startTime, 
        java.time.LocalDateTime endTime) throws Exception {
        List<Bar> bars = selectedDataSource.getBars(symbol, startTime, endTime);
        bars = aggregateByTime(bars, interval);
                
        System.out.println("Aggregation yeilded " + bars.size() + " entries");   

        return bars;
    }

    public DataSourceBase getSelectedDataSource() {
        return selectedDataSource;
    }

    public void onDataSourceChanged(DataSourceBase newDataSource) {
        selectedDataSource = newDataSource;
    }

    public void connectToFinam(FinamApiClient finamClient) {
        connectToFinam(finamClient, null);
    }

    public void connectToFinam(FinamApiClient finamClient, String secret) {
        try {      
            if (secret != null) {
                finamClient.setSecretToken(secret);
            }
            
            finamClient.connect();
            quoteList = Collections.unmodifiableList(finamClient.getQuotesList());
        } catch (Exception ex) {
            view.setError(ex);
        }
    }

    public List<Bar> aggregateByTime(List<Bar> bars, Interval interval) {
        if (interval == Interval.TICK || bars == null || bars.isEmpty()) {
            return bars;
        }

        List<Bar> result = new ArrayList<>();

        LocalDateTime bucketStart = bars.get(0).getTimestamp();
        LocalDateTime bucketEnd = bucketStart.plus(interval.duration);
        List<Bar> bucket = new ArrayList<>();

        for (Bar bar : bars) {
            boolean isNewBucket = bar.getTimestamp().isBefore(bucketEnd);
            if (!isNewBucket) {
                result.add(mean(bucket, bucketStart));
                bucket.clear();
                bucketStart = bar.getTimestamp();
                bucketEnd = bucketStart.plus(interval.duration);
            }
            bucket.add(bar);
        }

        if (!bucket.isEmpty()) {
            result.add(mean(bucket, bucketEnd));
        }

        return result;
    }

    private Bar mean(List<Bar> bars, LocalDateTime timestamp) {
        BigDecimal open = BigDecimal.ZERO;
        BigDecimal high = BigDecimal.ZERO;
        BigDecimal low = BigDecimal.ZERO;
        BigDecimal close = BigDecimal.ZERO;
        BigDecimal volume = BigDecimal.ZERO;

        for (Bar bar : bars) {
            open = open.add(bar.getOpen());
            high = high.add(bar.getHigh());
            low = low.add(bar.getLow());
            close = close.add(bar.getClose());
            volume = volume.add(bar.getVolume());
        }

        BigDecimal count = BigDecimal.valueOf(bars.size());

        return new Bar(
            timestamp,
            open.divide(count, SCALE, RoundingMode.HALF_UP),
            high.divide(count, SCALE, RoundingMode.HALF_UP),
            low.divide(count, SCALE, RoundingMode.HALF_UP),
            close.divide(count, SCALE, RoundingMode.HALF_UP),
            volume.divide(count, SCALE, RoundingMode.HALF_UP)
        );
    }


    private void initDataSourceList() {
        dataSourceList = new ArrayList<>();
        dataSourceList.add(new FinamApiClient());
    }
}
