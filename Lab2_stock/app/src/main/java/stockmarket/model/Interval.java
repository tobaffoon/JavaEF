package stockmarket.model;

public enum Interval {    
    TICKS("Тики"),
    ONE_MINUTE("1 минута"),
    FIVE_MINUTES("5 минут"),
    TEN_MINUTES("10 минут"),
    FIFTEEN_MINUTES("15 минут"),
    THIRTY_MINUTES("30 минут"),
    ONE_HOUR("1 час"),
    ONE_DAY("1 день"),
    ONE_WEEK("1 неделя"),
    ONE_MONTH("1 месяц");

    private final String displayName;
    
    private Interval(String displayName) {
        this.displayName = displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}