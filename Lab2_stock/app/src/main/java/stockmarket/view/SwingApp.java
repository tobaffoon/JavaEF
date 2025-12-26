package stockmarket.view;

import stockmarket.control.StockMarketController;
import stockmarket.io.DataSourceBase;
import stockmarket.io.FinamApiClient;
import stockmarket.model.Quote;
import stockmarket.model.Interval;
import stockmarket.utils.TimeUtils;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SwingApp implements StockMarketView {
    private static final String TIME_MASK = "##:##:##";
    private static final String DATE_MASK = "##.##.####";
    private static final String STATUS_READY = "Status: Ready";
    private static final Color SUCCESSFUL_TEXT_COLOR = new Color(0, 120, 0);
    private static final Color ERROR_TEXT_COLOR = new Color(200, 0, 0);
    private static final Color INFO_COLOR = Color.BLUE;

    private JFrame frame;
    private JComboBox<DataSourceBase> dataSourceCombo;
    private JLabel marketLabel;
    private JComboBox<Quote> quoteCombo;
    private JComboBox<Interval> intervalCombo;
    private JFormattedTextField beginDateTF;
    private JFormattedTextField endDateTF;
    private JFormattedTextField beginTimeTF;
    private JFormattedTextField endTimeTF;
    private JLabel statusLabel;
    private JLabel connectionLabel;
    private JButton connectButton;
    private JButton getDataButton;
    private JCheckBox emaCheckBox;
    private JCheckBox macdCheckBox;
    private JCheckBox smaCheckBox;
    private JTextField emaTSTF;
    private JTextField emaSFTF;
    private JTextField macdfTSTF;
    private JTextField macdsTSTF;
    private JTextField smaTSTF;

    private StockMarketController controller;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SwingApp().createAndShowGUI());
    }


    @Override
    public Interval getSelectedInterval() {
        return (Interval)intervalCombo.getSelectedItem();
    }

    @Override
    public void setBeginDate(LocalDateTime date) {
        String dateStr = TimeUtils.formatDate(date);
        String timeStr = TimeUtils.formatTime(date);
        beginDateTF.setText(dateStr);
        beginTimeTF.setText(timeStr);
    }

    @Override
    public void setEndDate(LocalDateTime date) {
        String dateStr = TimeUtils.formatDate(date);
        String timeStr = TimeUtils.formatTime(date);
        endDateTF.setText(dateStr);
        endTimeTF.setText(timeStr);
    }

    @Override
    public LocalDateTime getBeginDate() {
        String dateStr = beginDateTF.getText();
        String timeStr = beginTimeTF.getText();
        return TimeUtils.parseToLocalDateTime(dateStr, timeStr);
    }

    @Override
    public LocalDateTime getEndDate() {
        String dateStr = endDateTF.getText();
        String timeStr = endTimeTF.getText();
        return TimeUtils.parseToLocalDateTime(dateStr, timeStr);
    }

    @Override
    public void setDataSourceOptions(List<DataSourceBase> options) {
        dataSourceCombo.removeAllItems();
        for (DataSourceBase option : options) {
            dataSourceCombo.addItem(option);
        }
    }

    @Override
    public Quote getSelectedQuote() {
        return (Quote) quoteCombo.getSelectedItem();
    }

    @Override
    public void setQuoteOptions(List<Quote> quotes) {
        quoteCombo.removeAllItems();
        for (Quote quote : quotes) {
            quoteCombo.addItem(quote);
        }
    }

    @Override
    public void setConnectionStatus(String status, Color color) {
        connectionLabel.setText(status);
        connectionLabel.setForeground(color);
    }

    @Override
    public void setExecutionStatus(String status, Color color) {
        statusLabel.setText(status);
        statusLabel.setForeground(color);
    }

    @Override
    public void setError(Exception e) {
        setExecutionStatus("ERROR: " + e.getMessage(), ERROR_TEXT_COLOR);
    }

    private void createAndShowGUI() {
        controller = new StockMarketController(this);

        frame = new JFrame("Stock Market Analyzer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));

        // Control panel
        JPanel controlPanel = createControlPanel();
        frame.add(controlPanel, BorderLayout.NORTH);

        // Data and indicator settings
        // JPanel settingsPanel = createSettingsPanel();
        // frame.add(settingsPanel, BorderLayout.CENTER);

        // Status bar
        JPanel statusPanel = createStatusPanel();
        frame.add(statusPanel, BorderLayout.SOUTH);

        frame.setSize(900, 700);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Data Source Connection"));
        panel.add(Box.createVerticalStrut(10));

        // First row: Data source selection
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row1.add(new JLabel("Data Source:"));
        dataSourceCombo = new JComboBox<>();
        dataSourceCombo.addActionListener(e -> onDataSourceChanged());
        row1.add(dataSourceCombo);

        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> onConnectButtonClick());
        row1.add(connectButton);

        connectionLabel = new JLabel("Not Connected");
        connectionLabel.setForeground(ERROR_TEXT_COLOR);
        row1.add(connectionLabel);

        panel.add(row1);
        panel.add(Box.createVerticalStrut(5));

        // Second row: Market
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row2.add(new JLabel("Market:"));
        marketLabel = new JLabel("None");
        row2.add(marketLabel);

        panel.add(row2);
        panel.add(Box.createVerticalStrut(5));

        // Third row: Quote, Interval
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row3.add(new JLabel("Quote:"));
        quoteCombo = new JComboBox<>();
        quoteCombo.setEnabled(false);
        quoteCombo.addActionListener(e -> onQuoteChanged());
        row3.add(quoteCombo);

        panel.add(row3);
        panel.add(Box.createVerticalStrut(5));

        // Forth row: Interval and dates
        JPanel row4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row4.add(new JLabel("Interval:"));
        intervalCombo = new JComboBox<>();
        row4.add(intervalCombo);

        try{
            row4.add(new JLabel("Begin:"));
            MaskFormatter timeMask = new MaskFormatter(TIME_MASK);
            timeMask.setPlaceholderCharacter('0');
            beginTimeTF = new JFormattedTextField(timeMask);
            beginTimeTF.setValue("00:00:00");
            beginTimeTF.setColumns(10);

            MaskFormatter dateMask = new MaskFormatter(DATE_MASK);
            dateMask.setPlaceholderCharacter('0');
            beginDateTF = new JFormattedTextField(dateMask);
            beginDateTF.setValue("01.01.2024");
            beginDateTF.setColumns(10);
            row4.add(beginTimeTF);
            row4.add(beginDateTF);

            row4.add(new JLabel("End:"));
            timeMask.setPlaceholderCharacter('0');
            endTimeTF = new JFormattedTextField(timeMask);
            endTimeTF.setValue("23:59:59");
            endTimeTF.setColumns(10);

            dateMask.setPlaceholderCharacter('0');
            endDateTF = new JFormattedTextField(dateMask);
            endDateTF.setValue("31.12.2024");
            endDateTF.setColumns(10);
            row4.add(endTimeTF);
            row4.add(endDateTF);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        row4.add(Box.createHorizontalStrut(20));

        getDataButton = new JButton("Get Data");
        // getDataButton.setEnabled(false);
        getDataButton.addActionListener(e -> onGetDataButton());
        row4.add(getDataButton);

        panel.add(row4);
        panel.add(Box.createVerticalStrut(10));

        // Data inits
        initDataSourceList();
        initIntervals();

        return panel;
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Indicator Settings"));
        panel.add(Box.createVerticalStrut(10));

        // EMA Settings
        JPanel emaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        emaCheckBox = new JCheckBox("EMA Indicator", true);
        emaPanel.add(emaCheckBox);
        emaPanel.add(new JLabel("Time Series:"));
        emaTSTF = new JTextField("6", 5);
        emaPanel.add(emaTSTF);
        emaPanel.add(new JLabel("Scale Factor:"));
        emaSFTF = new JTextField("0.5", 5);
        emaPanel.add(emaSFTF);
        panel.add(emaPanel);
        panel.add(Box.createVerticalStrut(5));

        // MACD Settings
        JPanel macdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        macdCheckBox = new JCheckBox("MACD Indicator", true);
        macdPanel.add(macdCheckBox);
        macdPanel.add(new JLabel("Fast TS:"));
        macdfTSTF = new JTextField("12", 5);
        macdPanel.add(macdfTSTF);
        macdPanel.add(new JLabel("Slow TS:"));
        macdsTSTF = new JTextField("26", 5);
        macdPanel.add(macdsTSTF);
        panel.add(macdPanel);
        panel.add(Box.createVerticalStrut(5));

        // SMA Settings
        JPanel smaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        smaCheckBox = new JCheckBox("SMA Indicator", true);
        smaPanel.add(smaCheckBox);
        smaPanel.add(new JLabel("Time Series:"));
        smaTSTF = new JTextField("20", 5);
        smaPanel.add(smaTSTF);
        panel.add(smaPanel);
        panel.add(Box.createVerticalStrut(10));

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createEtchedBorder());

        statusLabel = new JLabel(STATUS_READY);
        statusLabel.setForeground(Color.BLUE);
        panel.add(statusLabel);

        return panel;
    }

    private void initDataSourceList() {
        ArrayList<DataSourceBase> dataSourceList = controller.getDataSourceList();
        for (DataSourceBase source : dataSourceList) {
            dataSourceCombo.addItem(source);
        }
        dataSourceCombo.setSelectedIndex(0);
    }

    private void initIntervals() {
        for (Interval interval : Interval.values()) {
            intervalCombo.addItem(interval);
        }
    }

    private void onDataSourceChanged() {
        DataSourceBase selectedSource = (DataSourceBase) dataSourceCombo.getSelectedItem();
        controller.onDataSourceChanged(selectedSource);
    }

    private void onQuoteChanged() {
        Quote selectedQuote = (Quote) quoteCombo.getSelectedItem();
        marketLabel.setText(selectedQuote.mic());
    }

    private void onConnectButtonClick(){
        if(controller.getSelectedDataSource() instanceof FinamApiClient finamClient){
            connectButton.setEnabled(false);
            quoteCombo.setEnabled(false);
            getDataButton.setEnabled(false);

            String secret = promptForFinamSecret();
            setConnectionStatus("Connecting...", INFO_COLOR);

            SwingUtilities.invokeLater(() -> {
                controller.connectToFinam(finamClient, secret);
                updateUIAfterConnection();
            });
        }
    }

    private void updateUIAfterConnection() {
        quoteCombo.setEnabled(true);
        getDataButton.setEnabled(true);
        List<Quote> quoteList = controller.getQuoteList();
        setQuoteOptions(quoteList);

        if (!quoteList.isEmpty()) {
            quoteCombo.setSelectedIndex(0);
        }

        setConnectionStatus("Connected!", SUCCESSFUL_TEXT_COLOR);
    }

    private void onGetDataButton() {
        Quote selectedQuote = getSelectedQuote();
        if(selectedQuote == null) {
            setExecutionStatus("No quote selected", ERROR_TEXT_COLOR);
            return;
        }
        try {
            LocalDateTime beginDate = getBeginDate();
            LocalDateTime endDate = getEndDate();
            Interval interval = getSelectedInterval();

            if(beginDate.isAfter(endDate)) {
                throw new Exception("Begin date must be before End date.");
            }
            if(!interval.isTimeSpanSufficient(beginDate, endDate)) {
                throw new Exception("Selected interval is too big for the specified time span.");
            }

            getDataButton.setEnabled(false);
            setExecutionStatus("Getting bars...", INFO_COLOR);
            String symbol = selectedQuote.symbol();

            SwingUtilities.invokeLater(() -> {
                try {
                    controller.getBars(symbol, interval, beginDate, endDate);
                    updateUIAfterBarsRequest();
                } catch (Exception ex) {
                    setError(ex);
                }
            });
        } catch (Exception ex) {
            setError(ex);
        }
        // try {

        //     if (currentDataSource == null) {
        //         updateStatus("ERROR: Not connected to data source", ERROR_TEXT_COLOR);
        //         return;
        //     }

        //     // Validate dates
        //     String beginDateStr = beginDateTF.getText();
        //     String endDateStr = endDateTF.getText();

        //     if (!TimeUtils.isValidDate(beginDateStr)) {
        //         updateStatus("ERROR: Begin date is invalid (format: dd.MM.yyyy)", ERROR_TEXT_COLOR);
        //         return;
        //     }
        //     if (!TimeUtils.isValidDate(endDateStr)) {
        //         updateStatus("ERROR: End date is invalid (format: dd.MM.yyyy)", ERROR_TEXT_COLOR);
        //         return;
        //     }

        //     updateStatus("Fetching data...", Color.BLUE);
        //     getDataButton.setEnabled(false);

        //     Thread dataThread = new Thread(() -> {
        //         try {
        //             String[] beginDate = beginDateStr.split("\\.");
        //             String[] endDate = endDateStr.split("\\.");

        //             currentDataSource.setBeginData(
        //                     Integer.parseInt(beginDate[0]),
        //                     Integer.parseInt(beginDate[1]) - 1,
        //                     Integer.parseInt(beginDate[2])
        //             );
        //             currentDataSource.setEndData(
        //                     Integer.parseInt(endDate[0]),
        //                     Integer.parseInt(endDate[1]) - 1,
        //                     Integer.parseInt(endDate[2])
        //             );

        //             currentDataSource.getData();

        //             JfreeCandlestickChart candlestickChart = new JfreeCandlestickChart("Stock Chart");
        //             String intervalValue = (String) intervalCombo.getSelectedItem();
        //             if (!candlestickChart.setInterval(intervalValue, intervalList, statusLabel)) {
        //                 throw new Exception("Invalid interval");
        //             }

        //             // Add trade data to chart
        //             for (var tradeData : currentDataSource.data) {
        //                 candlestickChart.onTrade(tradeData);
        //             }

        //             // Configure indicators
        //             candlestickChart.IsEma = emaCheckBox.isSelected();
        //             candlestickChart.IsMacd = macdCheckBox.isSelected();
        //             candlestickChart.IsSma = smaCheckBox.isSelected();

        //             if (candlestickChart.IsEma || candlestickChart.IsMacd || candlestickChart.IsSma) {
        //                 int emaTS = Integer.parseInt(emaTSTF.getText());
        //                 double emaSF = Double.parseDouble(emaSFTF.getText());
        //                 int macdFTS = Integer.parseInt(macdfTSTF.getText());
        //                 int macdSTS = Integer.parseInt(macdsTSTF.getText());
        //                 int smaTS = Integer.parseInt(smaTSTF.getText());

        //                 candlestickChart.fillIndicators(
        //                         currentDataSource.data,
        //                         emaTS, emaSF, macdFTS, macdSTS, smaTS
        //                 );
        //             }

        //             SwingUtilities.invokeLater(() -> {
        //                 String chartTitle = (String) marketCombo.getSelectedItem() + " " +
        //                         (String) quoteCombo.getSelectedItem() + " " +
        //                         beginDateStr + " - " + endDateStr;

        //                 JFreeChart chart = candlestickChart.createChart(chartTitle);
        //                 LegendTitle legend = new LegendTitle(chart.getPlot(),
        //                         new ColumnArrangement(HorizontalAlignment.CENTER, VerticalAlignment.CENTER, 0, 0),
        //                         new ColumnArrangement(HorizontalAlignment.CENTER, VerticalAlignment.CENTER, 0, 0)
        //                 );
        //                 legend.setPosition(RectangleEdge.BOTTOM);
        //                 legend.setHorizontalAlignment(HorizontalAlignment.CENTER);
        //                 legend.setBackgroundPaint(Color.WHITE);
        //                 legend.setFrame(new LineBorder());
        //                 legend.setMargin(0, 4, 5, 6);
        //                 chart.addLegend(legend);

        //                 JFrame chartFrame = new JFrame(chartTitle);
        //                 chartFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        
        //                 // Create a panel to hold the chart
        //                 JPanel chartPanel = new JPanel(new BorderLayout());
        //                 chartPanel.setBackground(Color.WHITE);
                        
        //                 // Use JFreeChart's built-in chart panel
        //                 org.jfree.chart.ChartPanel panel = new org.jfree.chart.ChartPanel(chart);
        //                 chartPanel.add(panel, BorderLayout.CENTER);
                        
        //                 chartFrame.add(chartPanel);
        //                 chartFrame.setSize(1000, 600);
        //                 chartFrame.setLocationRelativeTo(frame);
        //                 chartFrame.setVisible(true);

        //                 updateStatus("Data loaded and chart created successfully (" + currentDataSource.data.size() + " records)", SUCCESSFUL_TEXT_COLOR);
        //                 getDataButton.setEnabled(true);
        //             });
        //         } catch (Exception ex) {
        //             SwingUtilities.invokeLater(() -> {
        //                 updateStatus("ERROR: " + ex.getMessage(), ERROR_TEXT_COLOR);
        //                 getDataButton.setEnabled(true);
        //                 JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage(), "Data Loading Error", JOptionPane.ERROR_MESSAGE);
        //             });
        //         }
        //     });
        //     dataThread.setDaemon(true);
        //     dataThread.start();

        // } catch (Exception ex) {
        //     updateStatus("ERROR: " + ex.getMessage(), ERROR_TEXT_COLOR);
        //     getDataButton.setEnabled(true);
        //     JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        // }
    }

    private void updateUIAfterBarsRequest() {
        getDataButton.setEnabled(true);
        setExecutionStatus("Bars received", SUCCESSFUL_TEXT_COLOR);
    }

    private String promptForFinamSecret() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("Enter your Finam API Secret:");
        JPasswordField passwordField = new JPasswordField(40);

        panel.add(label);
        panel.add(Box.createVerticalStrut(5));
        panel.add(passwordField);
        panel.add(Box.createVerticalStrut(5));

        int result = JOptionPane.showConfirmDialog(
            frame,
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
}
