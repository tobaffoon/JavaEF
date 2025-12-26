package stockmarket.model;

import java.time.Duration;
import java.time.LocalDateTime;

public enum Interval {
    TICK("Тик", Duration.ofNanos(1)),
    ONE_MINUTE("1 минута", Duration.ofMinutes(1)),
    FIVE_MINUTES("5 минут", Duration.ofMinutes(5)),
    TEN_MINUTES("10 минут", Duration.ofMinutes(10)),
    FIFTEEN_MINUTES("15 минут", Duration.ofMinutes(15)),
    THIRTY_MINUTES("30 минут", Duration.ofMinutes(30)),
    ONE_HOUR("1 час", Duration.ofHours(1)),
    ONE_DAY("1 день", Duration.ofDays(1)),
    ONE_WEEK("1 неделя", Duration.ofDays(7)),
    ONE_MONTH("1 месяц", Duration.ofDays(30));

    public final Duration duration;
    private final String displayName;

    private Interval(String displayName, Duration duration) {
        this.displayName = displayName;
        this.duration = duration;
    }

    public boolean isTimeSpanSufficient(LocalDateTime begin, LocalDateTime end) {
        Duration timeSpan = Duration.between(begin, end);
        return timeSpan.compareTo(duration.multipliedBy(2)) >= 0;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
