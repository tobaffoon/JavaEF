package stockmarket.view;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import stockmarket.model.Bar;

import javax.swing.JPanel;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.List;

public final class CandlestickChartPanel extends JPanel {

    private final JFreeChart chart;

    public CandlestickChartPanel(String title, List<Bar> bars) {
        this.chart = buildChart(title, bars);
    }

    public JFreeChart getChart() {
        return chart;
    }

    private JFreeChart buildChart(String title, List<Bar> bars) {
        BarSeriesBuilder barBuilder = new BarSeriesBuilder(bars);

        OHLCSeriesCollection priceDataset = barBuilder.buildOhlcDataset();
        NumberAxis priceAxis = new NumberAxis("Price");
        priceAxis.setAutoRangeIncludesZero(false);

        CandlestickRenderer candleRenderer =
                new CandlestickRenderer(CandlestickRenderer.WIDTHMETHOD_AVERAGE);

        XYPlot pricePlot = new XYPlot(
                priceDataset,
                null,
                priceAxis,
                candleRenderer
        );
        pricePlot.setBackgroundPaint(Color.WHITE);

        TimeSeriesCollection volumeDataset = barBuilder.buildVolumeDataset();
        NumberAxis volumeAxis = new NumberAxis("Volume");
        volumeAxis.setAutoRangeIncludesZero(true);

        XYBarRenderer volumeRenderer = new XYBarRenderer();
        volumeRenderer.setShadowVisible(false);

        XYPlot volumePlot = new XYPlot(
                volumeDataset,
                null,
                volumeAxis,
                volumeRenderer
        );
        volumePlot.setBackgroundPaint(Color.WHITE);

        CombinedDomainXYPlot combinedPlot =
                new CombinedDomainXYPlot(createDateAxis());

        combinedPlot.add(pricePlot, 3);
        combinedPlot.add(volumePlot, 1);

        combinedPlot.setOrientation(PlotOrientation.VERTICAL);

        JFreeChart chart = new JFreeChart(
                title,
                JFreeChart.DEFAULT_TITLE_FONT,
                combinedPlot,
                true
        );
        chart.removeLegend();

        return chart;
    }

    private DateAxis createDateAxis() {
        DateAxis axis = new DateAxis("Time");
        axis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
        axis.setLowerMargin(0.02);
        axis.setUpperMargin(0.02);
        return axis;
    }
}
