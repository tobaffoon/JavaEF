package stockmarket.view;

import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import stockmarket.model.Bar;

import java.util.List;

final class BarSeriesBuilder {

    private final List<Bar> bars;

    BarSeriesBuilder(List<Bar> bars) {
        this.bars = bars;
    }

    OHLCSeriesCollection buildOhlcDataset() {
        OHLCSeries series = new OHLCSeries("Price");

        for (Bar bar : bars) {
            series.add(
                    new FixedMillisecond(
                            bar.timestamp()
                               .atZone(java.time.ZoneId.systemDefault())
                               .toInstant()
                               .toEpochMilli()
                    ),
                    bar.open().doubleValue(),
                    bar.high().doubleValue(),
                    bar.low().doubleValue(),
                    bar.close().doubleValue()
            );
        }

        OHLCSeriesCollection dataset = new OHLCSeriesCollection();
        dataset.addSeries(series);
        return dataset;
    }

    TimeSeriesCollection buildVolumeDataset() {
        TimeSeries volume = new TimeSeries("Volume");

        for (Bar bar : bars) {
            volume.add(
                    new FixedMillisecond(
                            bar.timestamp()
                               .atZone(java.time.ZoneId.systemDefault())
                               .toInstant()
                               .toEpochMilli()
                    ),
                    bar.volume().doubleValue()
            );
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(volume);
        return dataset;
    }
}
