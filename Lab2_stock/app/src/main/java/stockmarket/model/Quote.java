package stockmarket.model;

public record Quote(
    String name,
    String mic,
    String symbol
) {
    @Override
    public String toString() {
        return name;
    }
}
