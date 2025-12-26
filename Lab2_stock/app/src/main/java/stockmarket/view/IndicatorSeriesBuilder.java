package stockmarket.view;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import stockmarket.indicators.EMAIndicator;
import stockmarket.indicators.MACDIndicator;
import stockmarket.indicators.SMAIndicator;
import stockmarket.model.Bar;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

final class IndicatorSeriesBuilder {

    private final List<Bar> bars;
    private final IndicatorConfig cfg;

    IndicatorSeriesBuilder(List<Bar> bars, IndicatorConfig cfg) {
        this.bars = bars;
        this.cfg = cfg;
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

    private XYPlot buildEmaPlot() {
        TimeSeries ema = new TimeSeries("EMA");

        List<Double> closes = bars.stream()
                .map(b -> b.close().doubleValue())
                .toList();

        for (int i = 0; i + cfg.emaPeriod() < closes.size(); i++) {
            ema.add(
                    timestamp(i),
                    EMAIndicator.calculateEma(
                            closes,
                            cfg.emaSmoothing(),
                            i,
                            i + cfg.emaPeriod()
                    )
            );
        }

        TimeSeriesCollection ds = new TimeSeriesCollection(ema);
        return createLinePlot(ds, "EMA", Color.BLUE);
    }

    private XYPlot buildMacdSmaPlot() {
        TimeSeries macd = new TimeSeries("MACD");
        TimeSeries sma = new TimeSeries("SMA");

        List<Double> closes = bars.stream()
                .map(b -> b.close().doubleValue())
                .toList();

        for (int i = 0; i + cfg.slowMacd() < closes.size(); i++) {
            double m = MACDIndicator.calculateMacd(
                    closes,
                    i,
                    i + cfg.fastMacd(),
                    i + cfg.slowMacd()
            );
            macd.add(timestamp(i), m);
        }

        for (int i = 0; i + cfg.smaPeriod() < macd.getItemCount(); i++) {
            // sma.add(timestamp(i),
            //         SMAIndicator.calculateSma(
            //                 macd.getItems().stream()
            //                         .map(v -> v..doubleValue())
            //                         .toList(),
            //                 i,
            //                 cfg.smaPeriod()
            //         )
            // );
        }

        TimeSeriesCollection ds = new TimeSeriesCollection();
        if (cfg.macdEnabled()) ds.addSeries(macd);
        if (cfg.smaEnabled()) ds.addSeries(sma);

        return createLinePlot(ds, "MACD / SMA", Color.RED);
    }

    private XYPlot createLinePlot(TimeSeriesCollection ds, String name, Color color) {
        NumberAxis axis = new NumberAxis(name);
        XYLineAndShapeRenderer r = new XYLineAndShapeRenderer(true, false);
        r.setSeriesPaint(0, color);
        return new XYPlot(ds, null, axis, r);
    }

    private FixedMillisecond timestamp(int index) {
        return new FixedMillisecond(
                bars.get(index)
                    .timestamp()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
        );
    }
}
