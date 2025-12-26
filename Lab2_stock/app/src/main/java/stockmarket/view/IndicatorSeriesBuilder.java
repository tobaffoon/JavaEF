package stockmarket.view;

import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import stockmarket.indicators.Indicator;
import stockmarket.model.Bar;

import java.awt.*;
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
        if(closes.size() < indicator.warmupPeriod()) {
            throw new IllegalArgumentException(
                    "Not enough data to compute indicator: " + indicator.toString()
            );
        }
        
        TimeSeries series = new TimeSeries(indicator.toString());
        addIndicatorSeries(series, indicator);

        TimeSeriesCollection ds = new TimeSeriesCollection(series);
        return createLinePlot(ds, indicator.toString(), Color.BLUE);
    }

    public XYPlot buildPlot(List<Indicator> indicators) {
        TimeSeriesCollection ds = new TimeSeriesCollection();
        Color[] colors = {Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, Color.MAGENTA};
        for (int i = 0; i < indicators.size(); i++) {
            Indicator indicator = indicators.get(i);
            if(closes.size() < indicator.warmupPeriod()) {
                throw new IllegalArgumentException(
                        "Not enough data to compute indicator: " + indicator.toString()
                );
            }

            TimeSeries series = new TimeSeries(indicator.toString());
            addIndicatorSeries(series, indicator);
            ds.addSeries(series);
        }
        return createLinePlot(ds, "Indicators", colors);
    }

    private void addIndicatorSeries(TimeSeries target, Indicator indicator) {
        int start = indicator.warmupPeriod() - 1;

        for (int i = start; i < closes.size(); i++) {
            double value = indicator.compute(closes, i);
            target.add(timestamp(i), value);
        }
    }

    private XYPlot createLinePlot(TimeSeriesCollection ds, String axisName, Color color) {
        // Domain axis: DateAxis (timestamps)
        DateAxis domainAxis = new DateAxis("Time");
        // Range axis: NumberAxis (indicator values)
        NumberAxis rangeAxis = new NumberAxis(axisName);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, color);

        return new XYPlot(ds, domainAxis, rangeAxis, renderer);
    }

    private XYPlot createLinePlot(TimeSeriesCollection ds, String axisName, Color[] colors) {
        // Domain axis: DateAxis (timestamps)
        DateAxis domainAxis = new DateAxis("Time");
        // Range axis: NumberAxis (indicator values)
        NumberAxis rangeAxis = new NumberAxis(axisName);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        for (int i = 0; i < colors.length && i < ds.getSeriesCount(); i++) {
            renderer.setSeriesPaint(i, colors[i]);
        }

        return new XYPlot(ds, domainAxis, rangeAxis, renderer);
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
