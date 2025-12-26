package stockmarket.control;

import stockmarket.io.DataSourceBase;
import stockmarket.io.FinamApiClient;
import stockmarket.model.Quote;
import stockmarket.view.StockMarketView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StockMarketController {
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

    private void initDataSourceList() {
        dataSourceList = new ArrayList<>();
        dataSourceList.add(new FinamApiClient());
    }
}
