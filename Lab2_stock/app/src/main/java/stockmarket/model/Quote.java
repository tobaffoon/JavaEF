package stockmarket.model;

/**
 * Represents a financial asset (stock, bond, etc.) quote from the Finam API
 */
public class Quote {
    public String name;
    public String mic;
    public String symbol;

    public Quote(String name, String mic, String symbol) {
        this.name = name;
        this.mic = mic;
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return name;
    }
}
