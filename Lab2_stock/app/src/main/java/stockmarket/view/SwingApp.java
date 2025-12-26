package stockmarket.view;

import stockmarket.candlestick.JfreeCandlestickChart;
import stockmarket.control.StockMarketController;
import stockmarket.io.DataSourceBase;
import stockmarket.model.Quote;
import stockmarket.model.Interval;
import stockmarket.utils.TimeUtils;

import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.ColumnArrangement;
import org.jfree.chart.block.LineBorder;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.VerticalAlignment;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class SwingApp implements StockMarketView {
    private static final String TIME_MASK = "##:##:##";
    private static final String DATE_MASK = "##.##.####";
    private static final Color SUCCESSFUL_TEXT_COLOR = new Color(0, 120, 0);
    private static final Color ERROR_TEXT_COLOR = new Color(200, 0, 0);

    private JFrame frame;
    private JComboBox<DataSourceBase> dataSourceCombo;
    private JLabel marketLabel;
    private JComboBox<Quote> quoteCombo;
    private JComboBox<Interval> intervalCombo;
    private JFormattedTextField beginDateTF;
    private JFormattedTextField endDateTF;
    private JFormattedTextField beginTimeTF;
    private JFormattedTextField endTimeTF;
    private JLabel contractLabel;
    private JLabel statusLabel;
    private JLabel connectionLabel;
    private JProgressBar progressBar;
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

    private DataSourceBase currentDataSource;
    private ArrayList<String> intervalList;
    private StockMarketController controller;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SwingApp().createAndShowGUI());
    }

    private void createAndShowGUI() {
        // Initialize controller
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
        initDataSourceList();
        dataSourceCombo.addActionListener(e -> onDataSourceChanged());
        row1.add(dataSourceCombo);

        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> controller.onConnectButtonClick(e));
        row1.add(connectButton);

        connectionLabel = new JLabel("Not Connected");
        connectionLabel.setForeground(ERROR_TEXT_COLOR);
        row1.add(connectionLabel);

        progressBar = new JProgressBar(0, 100);
        progressBar.setMaximumSize(new Dimension(150, 20));
        row1.add(progressBar);

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

        row3.add(new JLabel("Contract:"));
        contractLabel = new JLabel("None");
        row3.add(contractLabel);

        panel.add(row3);
        panel.add(Box.createVerticalStrut(5));

        // Forth row: Interval and dates
        JPanel row4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row4.add(new JLabel("Interval:"));
        intervalCombo = new JComboBox<>();
        intervalCombo.setEnabled(false);
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
        getDataButton.setEnabled(false);
        getDataButton.addActionListener(this::onGetDataButton);
        row4.add(getDataButton);

        panel.add(row4);
        panel.add(Box.createVerticalStrut(10));

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

        statusLabel = new JLabel("Status: Ready");
        statusLabel.setForeground(Color.BLUE);
        panel.add(statusLabel);

        return panel;
    }

    private void initDataSourceList() {
        ArrayList<DataSourceBase> dataSourceList = controller.getDataSourceList();
        for (DataSourceBase source : dataSourceList) {
            dataSourceCombo.addItem(source);
        }
    }

    private void onDataSourceChanged() {
        resetConnectionState();
    }

    private void onQuoteChanged() {
        Quote selectedQuote = (Quote) quoteCombo.getSelectedItem();
        marketLabel.setText(selectedQuote.mic);
    }

    private void onGetDataButton(ActionEvent e) {
        throw new NotImplementedException("Data fetching not implemented yet.");
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

    public void resetConnectionState() {
        currentDataSource = null;
        marketLabel.setText("None");
        quoteCombo.removeAllItems();
        quoteCombo.setEnabled(false);
        intervalCombo.removeAllItems();
        intervalCombo.setEnabled(false);
        getDataButton.setEnabled(false);
        contractLabel.setText("None");
        progressBar.setValue(0);
        connectionLabel.setText("Not Connected");
        connectionLabel.setForeground(ERROR_TEXT_COLOR);
    }

    private void updateStatus(String message, Color color) {
        statusLabel.setText("Status: " + message);
        statusLabel.setForeground(color);
    }

    // Implementation of StockMarketView interface
    @Override
    public DataSourceBase getSelectedDataSource() {
        return (DataSourceBase) dataSourceCombo.getSelectedItem();
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
    public String getBeginDate() {
        return beginDateTF.getText();
    }

    @Override
    public void setBeginDate(String date) {
        beginDateTF.setText(date);
    }

    @Override
    public String getEndDate() {
        return endDateTF.getText();
    }

    @Override
    public void setEndDate(String date) {
        endDateTF.setText(date);
    }

    @Override
    public void setContract(String contract) {
        contractLabel.setText(contract);
    }

    @Override
    public void setStatus(String message) {
        statusLabel.setText("Status: " + message);
        // Determine color based on message content
        if (message.contains("ERROR")) {
            statusLabel.setForeground(ERROR_TEXT_COLOR);
        } else if (message.contains("Connected successfully")) {
            statusLabel.setForeground(SUCCESSFUL_TEXT_COLOR);
        } else {
            statusLabel.setForeground(Color.BLUE);
        }
    }

    @Override
    public void setConnectionStatus(String status) {
        connectionLabel.setText(status);
        // Determine color based on status content
        if (status.equals("Connected")) {
            connectionLabel.setForeground(SUCCESSFUL_TEXT_COLOR);
        } else if (status.equals("Failed")) {
            connectionLabel.setForeground(ERROR_TEXT_COLOR);
        } else {
            connectionLabel.setForeground(Color.BLUE);
        }
    }

    @Override
    public void setProgress(int value) {
        progressBar.setValue(value);
    }

    @Override
    public void enableConnectButton(boolean enabled) {
        connectButton.setEnabled(enabled);
    }

    @Override
    public void enableGetDataButton(boolean enabled) {
        getDataButton.setEnabled(enabled);
    }

    @Override
    public void showMessage(String message, String title) {
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public String promptForSecret() {
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

    @Override
    public boolean isEmaSelected() {
        return emaCheckBox != null && emaCheckBox.isSelected();
    }

    @Override
    public boolean isMacdSelected() {
        return macdCheckBox != null && macdCheckBox.isSelected();
    }

    @Override
    public boolean isSmaSelected() {
        return smaCheckBox != null && smaCheckBox.isSelected();
    }

    @Override
    public int getEmaTimeSeries() {
        try {
            return emaTSTF != null ? Integer.parseInt(emaTSTF.getText()) : 6;
        } catch (NumberFormatException e) {
            return 6;
        }
    }

    @Override
    public double getEmaScaleFactor() {
        try {
            return emaSFTF != null ? Double.parseDouble(emaSFTF.getText()) : 0.5;
        } catch (NumberFormatException e) {
            return 0.5;
        }
    }

    @Override
    public int getMacdFastTimeSeries() {
        try {
            return macdfTSTF != null ? Integer.parseInt(macdfTSTF.getText()) : 12;
        } catch (NumberFormatException e) {
            return 12;
        }
    }

    @Override
    public int getMacdSlowTimeSeries() {
        try {
            return macdsTSTF != null ? Integer.parseInt(macdsTSTF.getText()) : 26;
        } catch (NumberFormatException e) {
            return 26;
        }
    }

    @Override
    public int getSmaTimeSeries() {
        try {
            return smaTSTF != null ? Integer.parseInt(smaTSTF.getText()) : 20;
        } catch (NumberFormatException e) {
            return 20;
        }
    }
}
