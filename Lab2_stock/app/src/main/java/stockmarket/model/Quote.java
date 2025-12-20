package stockmarket.model;

/**
 * Represents a financial asset (stock, bond, etc.) quote from the Finam API
 */
public class Quote {
    public String ticker;
    public String isin;
    public String name;
    public String mic;

    public Quote() {
    }

    public Quote(String ticker, String isin, String name) {
        this.ticker = ticker;
        this.isin = isin;
        this.name = name;
    }

    public Quote(String ticker, String isin, String name, String mic) {
        this.ticker = ticker;
        this.isin = isin;
        this.name = name;
        this.mic = mic;
    }

    @Override
    public String toString() {
        return name != null ? name : ticker;
    }
}
