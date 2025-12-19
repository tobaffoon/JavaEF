package stockmarket.model;

/**
 * Represents a financial asset (stock, bond, etc.) quote from the Finam API
 */
public class Quote {
    private String ticker;
    private String isin;
    private String name;

    public Quote() {
    }

    public Quote(String ticker, String isin, String name) {
        this.ticker = ticker;
        this.isin = isin;
        this.name = name;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name != null ? name : ticker;
    }
}
