package stockmarket.view;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import stockmarket.indicators.Indicator;
import stockmarket.model.Bar;

import java.awt.Color;
import java.time.ZoneId;
import java.util.List;

public class IndicatorSeriesBuilder {

    private final List<Bar> bars;
    private final List<Double> closes;

    public IndicatorSeriesBuilder(List<Bar> bars) {
        this.bars = bars;
        this.closes = bars.stream()
                .map(b -> b.close().doubleValue())
                .toList();
    }

    public XYPlot buildPlot(Indicator indicator) {
        TimeSeries series = new TimeSeries(indicator.toString());

        addIndicatorSeries(series, indicator);

        return createLinePlot(
                new TimeSeriesCollection(series),
                indicator.toString(),
                Color.BLUE
        );
    }

    private void addIndicatorSeries(TimeSeries target, Indicator indicator) {
        int start = indicator.warmupPeriod() - 1;

        for (int i = start; i < closes.size(); i++) {
            double value = indicator.compute(closes, i);
            target.add(timestamp(i), value);
        }
    }

    // private void addIndicatorSeries(
    //         TimeSeries target,
    //         Indicator indicator,
    //         TimeSeries source
    // ) {
    //     List<Double> values = source.getItems().stream()
    //             .map(i -> ((Number) i).doubleValue())
    //             .toList();

    //     int start = indicator.warmupPeriod() - 1;

    //     for (int i = start; i < values.size(); i++) {
    //         double v = indicator.compute(values, i);
    //         target.add(source.getTimePeriod(i), v);
    //     }
    // }

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
