package stockmarket.view;

import stockmarket.candlestick.JfreeCandlestickChart;
import stockmarket.control.StockMarketController;
import stockmarket.datasource.DataSourceBase;
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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

public class SwingApp {
    private static final Color SUCCESSFUL_TEXT_COLOR = new Color(0, 120, 0);
    private static final Color ERROR_TEXT_COLOR = new Color(200, 0, 0);

    private JFrame frame;
    private JComboBox<String> dataSourceCombo;
    private JComboBox<String> marketCombo;
    private JComboBox<String> quoteCombo;
    private JComboBox<String> intervalCombo;
    private JTextField beginDateTF;
    private JTextField endDateTF;
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

        // Second row: Market, Quote, Interval
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row2.add(new JLabel("Market:"));
        marketCombo = new JComboBox<>();
        marketCombo.setEnabled(false);
        marketCombo.addActionListener(e -> controller.onMarketChanged());
        row2.add(marketCombo);

        row2.add(new JLabel("Quote:"));
        quoteCombo = new JComboBox<>();
        quoteCombo.setEnabled(false);
        quoteCombo.addActionListener(e -> controller.onQuoteChanged());
        row2.add(quoteCombo);

        row2.add(new JLabel("Contract:"));
        contractLabel = new JLabel("None");
        row2.add(contractLabel);

        panel.add(row2);
        panel.add(Box.createVerticalStrut(5));

        // Third row: Interval and dates
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row3.add(new JLabel("Interval:"));
        intervalCombo = new JComboBox<>();
        intervalCombo.setEnabled(false);
        intervalCombo.addActionListener(e -> controller.onIntervalChanged());
        row3.add(intervalCombo);

        row3.add(new JLabel("Begin Date:"));
        beginDateTF = new JTextField("01.01.2024", 10);
        row3.add(beginDateTF);

        row3.add(new JLabel("End Date:"));
        endDateTF = new JTextField("31.12.2024", 10);
        row3.add(endDateTF);

        row3.add(Box.createHorizontalStrut(20));

        getDataButton = new JButton("Get Data");
        getDataButton.setEnabled(false);
        getDataButton.addActionListener(this::onGetDataButton);
        row3.add(getDataButton);

        panel.add(row3);
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
        ArrayList<String> dataSourceList = controller.getDataSourceList();
        dataSourceList.add("Finam API");
        for (String source : dataSourceList) {
            dataSourceCombo.addItem(source);
        }
    }

    private void onDataSourceChanged() {
        resetConnectionState();
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
        marketCombo.removeAllItems();
        marketCombo.setEnabled(false);
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

    // Getter methods for controller access
    public JFrame getFrame() {
        return frame;
    }

    public JComboBox<String> getDataSourceCombo() {
        return dataSourceCombo;
    }

    public JComboBox<String> getMarketCombo() {
        return marketCombo;
    }

    public JComboBox<String> getQuoteCombo() {
        return quoteCombo;
    }

    public JComboBox<String> getIntervalCombo() {
        return intervalCombo;
    }

    public JTextField getBeginDateTF() {
        return beginDateTF;
    }

    public JTextField getEndDateTF() {
        return endDateTF;
    }

    public JLabel getContractLabel() {
        return contractLabel;
    }

    public JLabel getStatusLabel() {
        return statusLabel;
    }

    public JLabel getConnectionLabel() {
        return connectionLabel;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public JButton getConnectButton() {
        return connectButton;
    }

    public JButton getGetDataButton() {
        return getDataButton;
    }

    public JCheckBox getEmaCheckBox() {
        return emaCheckBox;
    }

    public JCheckBox getMacdCheckBox() {
        return macdCheckBox;
    }

    public JCheckBox getSmaCheckBox() {
        return smaCheckBox;
    }

    public JTextField getEmaTSTF() {
        return emaTSTF;
    }

    public JTextField getEmaSFTF() {
        return emaSFTF;
    }

    public JTextField getMacdfTSTF() {
        return macdfTSTF;
    }

    public JTextField getMacdsTSTF() {
        return macdsTSTF;
    }

    public JTextField getSmaTSTF() {
        return smaTSTF;
    }
}
