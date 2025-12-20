package stockmarket.control;

import stockmarket.candlestick.JfreeCandlestickChart;
import stockmarket.datasource.DataSourceBase;
import stockmarket.datasource.FinamApiClient;
import stockmarket.model.Quote;
import stockmarket.utils.TimeUtils;
import stockmarket.view.SwingApp;

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

public class StockMarketController {
    private static final Color SUCCESSFUL_TEXT_COLOR = new Color(0, 120, 0);
    private static final Color ERROR_TEXT_COLOR = new Color(200, 0, 0);

    private SwingApp view;
    private ArrayList<DataSourceBase> dataSourceList;
    private ArrayList<Quote> quoteList;
    private ArrayList<String> intervalList;

    public StockMarketController(SwingApp view) {
        this.view = view;
        initDataSourceList();
    }
    
    public ArrayList<DataSourceBase> getDataSourceList(){
        return dataSourceList;
    }

    private void initDataSourceList() {
        dataSourceList = new ArrayList<>();
        dataSourceList.add(new FinamApiClient());
    }

    public void onDataSourceChanged() {
        // Reset connection state through view
        view.resetConnectionState();
    }

    private DataSourceBase getSelectedDataSource() {
        DataSourceBase selectedSource = (DataSourceBase)view.getDataSourceCombo().getSelectedItem();
        return selectedSource;
    }

    public void onConnectButtonClick(ActionEvent e) {
        DataSourceBase selectedSource = (DataSourceBase)getSelectedDataSource();
        if (selectedSource == null) {
            updateStatus("ERROR: Please select a data source", ERROR_TEXT_COLOR);
            return;
        }

        view.getConnectButton().setEnabled(false);
        view.getProgressBar().setValue(10);
        updateStatus("Connecting...", Color.BLUE);
        view.getConnectionLabel().setText("Connecting...");

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
                
                view.getProgressBar().setValue(30);
                selectedSource.connect();
                view.getProgressBar().setValue(50);

                view.getProgressBar().setValue(60);
                quoteList = selectedSource.getQuotesList();
                view.getProgressBar().setValue(70);
                intervalList = selectedSource.getIntervalList();
                view.getProgressBar().setValue(80);

                SwingUtilities.invokeLater(this::updateUIAfterConnection);
                view.getProgressBar().setValue(100);
                updateStatus("Connected successfully", SUCCESSFUL_TEXT_COLOR);
                view.getConnectionLabel().setText("Connected");
                view.getConnectionLabel().setForeground(SUCCESSFUL_TEXT_COLOR);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    updateStatus("ERROR: " + ex.getMessage(), ERROR_TEXT_COLOR);
                    view.getConnectionLabel().setText("Failed");
                    view.getConnectionLabel().setForeground(ERROR_TEXT_COLOR);
                    view.getProgressBar().setValue(0);
                    view.getConnectButton().setEnabled(true);
                });
            }
        });
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    private void updateUIAfterConnection() {
        view.getQuoteCombo().removeAllItems();
        for (Quote quote : quoteList) {
            view.getQuoteCombo().addItem(quote);
        }
        view.getQuoteCombo().setEnabled(true);

        view.getIntervalCombo().removeAllItems();
        for (String interval : intervalList) {
            view.getIntervalCombo().addItem(interval);
        }
        view.getIntervalCombo().setEnabled(true);

        view.getGetDataButton().setEnabled(true);
        view.getConnectButton().setEnabled(true);

        if (view.getQuoteCombo().getItemCount() > 0) {
            view.getQuoteCombo().setSelectedIndex(0);
        }

        if (view.getIntervalCombo().getItemCount() > 0) {
            view.getIntervalCombo().setSelectedIndex(0);
        }
    }

    public void onGetDataButton(ActionEvent e) {
        try {
            DataSourceBase currentDataSource = (DataSourceBase)getSelectedDataSource();
            if (currentDataSource == null) {
                updateStatus("ERROR: Not connected to data source", ERROR_TEXT_COLOR);
                return;
            }

            // Validate dates
            String beginDateStr = view.getBeginDateTF().getText();
            String endDateStr = view.getEndDateTF().getText();

            if (!TimeUtils.isValidDate(beginDateStr)) {
                updateStatus("ERROR: Begin date is invalid (format: dd.MM.yyyy)", ERROR_TEXT_COLOR);
                return;
            }
            if (!TimeUtils.isValidDate(endDateStr)) {
                updateStatus("ERROR: End date is invalid (format: dd.MM.yyyy)", ERROR_TEXT_COLOR);
                return;
            }

            updateStatus("Fetching data...", Color.BLUE);
            view.getGetDataButton().setEnabled(false);

            Thread dataThread = new Thread(() -> {
                try {
                    String[] beginDate = beginDateStr.split("\\.");
                    String[] endDate = endDateStr.split("\\.");

                    JfreeCandlestickChart candlestickChart = new JfreeCandlestickChart("Stock Chart");
                    String intervalValue = (String) view.getIntervalCombo().getSelectedItem();
                    if (!candlestickChart.setInterval(intervalValue, intervalList, view.getStatusLabel())) {
                        throw new Exception("Invalid interval");
                    }

                    // Add trade data to chart
                    for (var tradeData : currentDataSource.data) {
                        candlestickChart.onTrade(tradeData);
                    }

                    // Configure indicators
                    candlestickChart.IsEma = view.getEmaCheckBox().isSelected();
                    candlestickChart.IsMacd = view.getMacdCheckBox().isSelected();
                    candlestickChart.IsSma = view.getSmaCheckBox().isSelected();

                    if (candlestickChart.IsEma || candlestickChart.IsMacd || candlestickChart.IsSma) {
                        int emaTS = Integer.parseInt(view.getEmaTSTF().getText());
                        double emaSF = Double.parseDouble(view.getEmaSFTF().getText());
                        int macdFTS = Integer.parseInt(view.getMacdfTSTF().getText());
                        int macdSTS = Integer.parseInt(view.getMacdsTSTF().getText());
                        int smaTS = Integer.parseInt(view.getSmaTSTF().getText());

                        candlestickChart.fillIndicators(
                                currentDataSource.data,
                                emaTS, emaSF, macdFTS, macdSTS, smaTS
                        );
                    }

                    SwingUtilities.invokeLater(() -> {
                        Quote selectedQuote = (Quote) view.getQuoteCombo().getSelectedItem();
                        String chartTitle = selectedQuote.mic + " " +
                                selectedQuote + " " +
                                beginDateStr + " - " + endDateStr;

                        JFreeChart chart = candlestickChart.createChart(chartTitle);
                        LegendTitle legend = new LegendTitle(chart.getPlot(),
                                new ColumnArrangement(HorizontalAlignment.CENTER, VerticalAlignment.CENTER, 0, 0),
                                new ColumnArrangement(HorizontalAlignment.CENTER, VerticalAlignment.CENTER, 0, 0)
                        );
                        legend.setPosition(RectangleEdge.BOTTOM);
                        legend.setHorizontalAlignment(HorizontalAlignment.CENTER);
                        legend.setBackgroundPaint(Color.WHITE);
                        legend.setFrame(new LineBorder());
                        legend.setMargin(0, 4, 5, 6);
                        chart.addLegend(legend);

                        JFrame chartFrame = new JFrame(chartTitle);
                        chartFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                        // Create a panel to hold the chart
                        JPanel chartPanel = new JPanel(new BorderLayout());
                        chartPanel.setBackground(Color.WHITE);

                        // Use JFreeChart's built-in chart panel
                        org.jfree.chart.ChartPanel panel = new org.jfree.chart.ChartPanel(chart);
                        chartPanel.add(panel, BorderLayout.CENTER);

                        chartFrame.add(chartPanel);
                        chartFrame.setSize(1000, 600);
                        chartFrame.setLocationRelativeTo(view.getFrame());
                        chartFrame.setVisible(true);

                        updateStatus("Data loaded and chart created successfully (" + currentDataSource.data.size() + " records)", SUCCESSFUL_TEXT_COLOR);
                        view.getGetDataButton().setEnabled(true);
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        updateStatus("ERROR: " + ex.getMessage(), ERROR_TEXT_COLOR);
                        view.getGetDataButton().setEnabled(true);
                        JOptionPane.showMessageDialog(view.getFrame(), "Error: " + ex.getMessage(), "Data Loading Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            });
            dataThread.setDaemon(true);
            dataThread.start();

        } catch (Exception ex) {
            updateStatus("ERROR: " + ex.getMessage(), ERROR_TEXT_COLOR);
            view.getGetDataButton().setEnabled(true);
            JOptionPane.showMessageDialog(view.getFrame(), "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetConnectionState() {
        view.getQuoteCombo().removeAllItems();
        view.getQuoteCombo().setEnabled(false);
        view.getIntervalCombo().removeAllItems();
        view.getIntervalCombo().setEnabled(false);
        view.getGetDataButton().setEnabled(false);
        view.getContractLabel().setText("None");
        view.getProgressBar().setValue(0);
        view.getConnectionLabel().setText("Not Connected");
        view.getConnectionLabel().setForeground(ERROR_TEXT_COLOR);
    }

    private String promptForFinamSecret() {
        // Create a custom dialog for secret input
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("Enter your Finam API Secret:");
        JPasswordField passwordField = new JPasswordField(40);

        panel.add(label);
        panel.add(Box.createVerticalStrut(5));
        panel.add(passwordField);
        panel.add(Box.createVerticalStrut(5));

        int result = JOptionPane.showConfirmDialog(
            view.getFrame(),
            panel,
            "Finam API Authentication",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.INFORMATION_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            return new String(passwordField.getPassword());
        }
        return null;
    }

    private void updateStatus(String message, Color color) {
        view.getStatusLabel().setText("Status: " + message);
        view.getStatusLabel().setForeground(color);
    }
}
