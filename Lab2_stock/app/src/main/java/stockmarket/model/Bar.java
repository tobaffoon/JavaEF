package stockmarket.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Bar(
    LocalDateTime timestamp,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    BigDecimal volume
) {}
