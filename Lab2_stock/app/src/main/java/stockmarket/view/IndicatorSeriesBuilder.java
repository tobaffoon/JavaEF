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
        TimeSeries series = new TimeSeries(indicator.toString());
        addIndicatorSeries(series, indicator);

        TimeSeriesCollection ds = new TimeSeriesCollection(series);
        return createLinePlot(ds, indicator.toString(), Color.BLUE);
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
        rangeAxis.setAutoRangeIncludesZero(false);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, color);

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
