package stockmarket.view;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import stockmarket.indicators.EMAIndicator;
import stockmarket.indicators.Indicator;
import stockmarket.indicators.MACDIndicator;
import stockmarket.indicators.SMAIndicator;
import stockmarket.model.Bar;

import java.awt.Color;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

final class IndicatorSeriesBuilder {

    private final List<Bar> bars;
    private final IndicatorConfig cfg;
    private final List<Double> closes;

    IndicatorSeriesBuilder(List<Bar> bars, IndicatorConfig cfg) {
        this.bars = bars;
        this.cfg = cfg;
        this.closes = bars.stream()
                .map(b -> b.close().doubleValue())
                .toList();
    }

    List<XYPlot> buildPlots() {
        List<XYPlot> plots = new ArrayList<>();

        if (cfg.emaEnabled()) {
            plots.add(buildEmaPlot());
        }

        if (cfg.macdEnabled() || cfg.smaEnabled()) {
            plots.add(buildMacdSmaPlot());
        }

        return plots;
    }

    /* ================= EMA ================= */

    private XYPlot buildEmaPlot() {
        Indicator ema = new EMAIndicator(cfg.emaPeriod());
        TimeSeries series = new TimeSeries("EMA");

        addIndicatorSeries(series, ema);

        return createLinePlot(
                new TimeSeriesCollection(series),
                "EMA",
                Color.BLUE
        );
    }

    /* ================= MACD + SMA ================= */

    private XYPlot buildMacdSmaPlot() {
        TimeSeries macdSeries = new TimeSeries("MACD");
        TimeSeries smaSeries = new TimeSeries("Signal");

        Indicator macd = new MACDIndicator(
                cfg.fastMacd(),
                cfg.slowMacd()
        );

        addIndicatorSeries(macdSeries, macd);

        if (cfg.smaEnabled()) {
            Indicator signal = new SMAIndicator(cfg.smaPeriod());
            addIndicatorSeries(smaSeries, signal, macdSeries);
        }

        TimeSeriesCollection ds = new TimeSeriesCollection();
        if (cfg.macdEnabled()) { 
            ds.addSeries(macdSeries);
        }
        if (cfg.smaEnabled()) {
            ds.addSeries(smaSeries);
        } 

        return createLinePlot(ds, "MACD", Color.RED);
    }

    /* ================= Helpers ================= */

    private void addIndicatorSeries(TimeSeries target, Indicator indicator) {
        int start = indicator.warmupPeriod() - 1;

        for (int i = start; i < closes.size(); i++) {
            double value = indicator.compute(closes, i);
            target.add(timestamp(i), value);
        }
    }

    /**
     * Applies indicator to another TimeSeries (e.g. SMA over MACD).
     */
    private void addIndicatorSeries(
            TimeSeries target,
            Indicator indicator,
            TimeSeries source
    ) {
        List<Double> values = source.getItems().stream()
                .map(i -> ((Number) i).doubleValue())
                .toList();

        int start = indicator.warmupPeriod() - 1;

        for (int i = start; i < values.size(); i++) {
            double v = indicator.compute(values, i);
            target.add(source.getTimePeriod(i), v);
        }
    }

    private XYPlot createLinePlot(
            TimeSeriesCollection ds,
            String axisName,
            Color color
    ) {
        NumberAxis axis = new NumberAxis(axisName);
        XYLineAndShapeRenderer r =
                new XYLineAndShapeRenderer(true, false);

        r.setSeriesPaint(0, color);
        return new XYPlot(ds, null, axis, r);
    }

    private FixedMillisecond timestamp(int index) {
        return new FixedMillisecond(
                bars.get(index)
                        .timestamp()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
        );
    }
}
