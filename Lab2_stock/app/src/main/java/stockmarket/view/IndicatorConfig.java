package stockmarket.view;

public record IndicatorConfig(
        boolean emaEnabled,
        boolean macdEnabled,
        boolean smaEnabled,
        int emaPeriod,
        double emaSmoothing,
        int fastMacd,
        int slowMacd,
        int smaPeriod
) {}
