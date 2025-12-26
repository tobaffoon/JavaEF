package stockmarket.control;

import stockmarket.candlestick.JfreeCandlestickChart;
import stockmarket.io.DataSourceBase;
import stockmarket.io.FinamApiClient;
import stockmarket.model.Quote;
import stockmarket.utils.TimeUtils;
import stockmarket.view.StockMarketView;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.ColumnArrangement;
import org.jfree.chart.block.LineBorder;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.VerticalAlignment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class StockMarketController {
    private StockMarketView view;
    private ArrayList<DataSourceBase> dataSourceList;
    private ArrayList<Quote> quoteList;
    private List<String> intervalList;

    public StockMarketController(StockMarketView view) {
        this.view = view;
        initDataSourceList();
    }
    
    public ArrayList<DataSourceBase> getDataSourceList(){
        return dataSourceList;
    }

    public void onDataSourceChanged() {
        view.resetConnectionState();
        view.setDataSourceOptions(dataSourceList);
    }

    public void onConnectButtonClick(ActionEvent e) {
        DataSourceBase selectedSource = (DataSourceBase)getSelectedDataSource();
        if (selectedSource == null) {
            updateStatus("ERROR: Please select a data source");
            return;
        }

        view.enableConnectButton(false);
        view.setProgress(10);
        view.setStatus("Connecting...");
        view.setConnectionStatus("Connecting...");

        Thread connectionThread = new Thread(() -> {
            try {
                if(selectedSource instanceof FinamApiClient finamClient){          
                    String storedSecret = finamClient.getToken();
                    if (storedSecret == null || storedSecret.isEmpty()) {
                        // Prompt for secret on UI thread
                        String[] secretResult = new String[1];
                        SwingUtilities.invokeAndWait(() -> {
                            secretResult[0] = promptForFinamSecret();
                        });

                        if (secretResult[0] == null || secretResult[0].isEmpty()) {
                            throw new InterruptedException("Finam API secret is required for connection");
                        }
                        finamClient.setSecretToken(secretResult[0]);
                    }
                }
                
                view.setProgress(30);
                selectedSource.connect();
                view.setProgress(50);

                view.setProgress(60);
                quoteList = selectedSource.getQuotesList();
                view.setProgress(70);
                intervalList = selectedSource.getIntervalList();
                view.setProgress(80);

                SwingUtilities.invokeLater(this::updateUIAfterConnection);
                view.setProgress(100);
                view.setStatus("Connected successfully");
                view.setConnectionStatus("Connected");
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    view.setStatus("ERROR: " + ex.getMessage());
                    view.setConnectionStatus("Failed");
                    view.setProgress(0);
                    view.enableConnectButton(true);
                });
            }
        });
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    public void onGetDataButton(ActionEvent e) {
        try {
            DataSourceBase currentDataSource = (DataSourceBase)getSelectedDataSource();
            if (currentDataSource == null) {
                updateStatus("ERROR: Not connected to data source");
                return;
            }

            // Validate dates
            String beginDateStr = view.getBeginDate();
            String endDateStr = view.getEndDate();

            if (!TimeUtils.isValidDate(beginDateStr)) {
                updateStatus("ERROR: Begin date is invalid (format: dd.MM.yyyy)");
                return;
            }
            if (!TimeUtils.isValidDate(endDateStr)) {
                updateStatus("ERROR: End date is invalid (format: dd.MM.yyyy)");
                return;
            }

            updateStatus("Fetching data...");
            view.enableGetDataButton(false);

            Thread dataThread = new Thread(() -> {
                // try {
                //     String[] beginDate = beginDateStr.split("\\.");
                //     String[] endDate = endDateStr.split("\\.");

                //     JfreeCandlestickChart candlestickChart = new JfreeCandlestickChart("Stock Chart");
                //     String intervalValue = (String) view.getIntervalCombo().getSelectedItem();
                //     if (!candlestickChart.setInterval(intervalValue, intervalList, view.getStatusLabel())) {
                //         throw new Exception("Invalid interval");
                //     }

                //     // Add trade data to chart
                //     for (var tradeData : currentDataSource.data) {
                //         candlestickChart.onTrade(tradeData);
                //     }

                //     // Configure indicators
                //     candlestickChart.IsEma = view.getEmaCheckBox().isSelected();
                //     candlestickChart.IsMacd = view.getMacdCheckBox().isSelected();
                //     candlestickChart.IsSma = view.getSmaCheckBox().isSelected();

                //     if (candlestickChart.IsEma || candlestickChart.IsMacd || candlestickChart.IsSma) {
                //         int emaTS = Integer.parseInt(view.getEmaTSTF().getText());
                //         double emaSF = Double.parseDouble(view.getEmaSFTF().getText());
                //         int macdFTS = Integer.parseInt(view.getMacdfTSTF().getText());
                //         int macdSTS = Integer.parseInt(view.getMacdsTSTF().getText());
                //         int smaTS = Integer.parseInt(view.getSmaTSTF().getText());

                //         candlestickChart.fillIndicators(
                //                 currentDataSource.data,
                //                 emaTS, emaSF, macdFTS, macdSTS, smaTS
                //         );
                //     }

                //     SwingUtilities.invokeLater(() -> {
                //         Quote selectedQuote = (Quote) view.getQuoteCombo().getSelectedItem();
                //         String chartTitle = selectedQuote.mic + " " +
                //                 selectedQuote + " " +
                //                 beginDateStr + " - " + endDateStr;

                //         JFreeChart chart = candlestickChart.createChart(chartTitle);
                //         LegendTitle legend = new LegendTitle(chart.getPlot(),
                //                 new ColumnArrangement(HorizontalAlignment.CENTER, VerticalAlignment.CENTER, 0, 0),
                //                 new ColumnArrangement(HorizontalAlignment.CENTER, VerticalAlignment.CENTER, 0, 0)
                //         );
                //         legend.setPosition(RectangleEdge.BOTTOM);
                //         legend.setHorizontalAlignment(HorizontalAlignment.CENTER);
                //         legend.setBackgroundPaint(Color.WHITE);
                //         legend.setFrame(new LineBorder());
                //         legend.setMargin(0, 4, 5, 6);
                //         chart.addLegend(legend);

                //         JFrame chartFrame = new JFrame(chartTitle);
                //         chartFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                //         // Create a panel to hold the chart
                //         JPanel chartPanel = new JPanel(new BorderLayout());
                //         chartPanel.setBackground(Color.WHITE);

                //         // Use JFreeChart's built-in chart panel
                //         org.jfree.chart.ChartPanel panel = new org.jfree.chart.ChartPanel(chart);
                //         chartPanel.add(panel, BorderLayout.CENTER);

                //         chartFrame.add(chartPanel);
                //         chartFrame.setSize(1000, 600);
                //         chartFrame.setLocationRelativeTo(view.getFrame());
                //         chartFrame.setVisible(true);

                //         updateStatus("Data loaded and chart created successfully (" + currentDataSource.data.size() + " records)", SUCCESSFUL_TEXT_COLOR);
                //         view.getGetDataButton().setEnabled(true);
                //     });
                // } catch (Exception ex) {
                //     SwingUtilities.invokeLater(() -> {
                //         updateStatus("ERROR: " + ex.getMessage(), ERROR_TEXT_COLOR);
                //         view.getGetDataButton().setEnabled(true);
                //         JOptionPane.showMessageDialog(view.getFrame(), "Error: " + ex.getMessage(), "Data Loading Error", JOptionPane.ERROR_MESSAGE);
                //     });
                // }
            });
            dataThread.setDaemon(true);
            dataThread.start();

        } catch (Exception ex) {
            view.setStatus("ERROR: " + ex.getMessage());
            view.enableGetDataButton(true);
            view.showMessage("Error: " + ex.getMessage(), "Error");
        }
    }

    private void initDataSourceList() {
        dataSourceList = new ArrayList<>();
        dataSourceList.add(new FinamApiClient());
    }
    
    private void resetConnectionState() {
        view.setQuoteOptions(java.util.Collections.emptyList());
        view.enableQuoteCombo(false);
        view.setIntervalOptions(java.util.Collections.emptyList());
        view.enableIntervalCombo(false);
        view.enableGetDataButton(false);
        view.setContract("None");
        view.setProgress(0);
        view.setConnectionStatus("Not Connected");
    }

    private String promptForFinamSecret() {
        return view.promptForSecret();
    }

    private void updateUIAfterConnection() {
        view.setQuoteOptions(quoteList);
        view.enableQuoteCombo(true);

        view.setIntervalOptions(intervalList);
        view.enableIntervalCombo(true);

        view.enableGetDataButton(true);
        view.enableConnectButton(true);

        if (!quoteList.isEmpty()) {
            view.setSelectedQuoteIndex(0);
        }

        if (!intervalList.isEmpty()) {
            view.setSelectedIntervalIndex(0);
        }
    }

    private void updateStatus(String message) {
        view.setStatus("Status: " + message);
    }

    private DataSourceBase getSelectedDataSource() {
        return view.getSelectedDataSource();
    }
}
