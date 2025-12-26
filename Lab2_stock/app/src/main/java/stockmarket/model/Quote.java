package stockmarket.model;

/**
 * Represents a financial asset (stock, bond, etc.) quote from the Finam API
 */
public class Quote {
    public String name;
    public String mic;

    public Quote(String name, String mic) {
        this.name = name;
        this.mic = mic;
    }

    @Override
    public String toString() {
        return name;
    }
}
