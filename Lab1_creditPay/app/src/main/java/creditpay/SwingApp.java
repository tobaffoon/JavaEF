package creditpay;

import creditpay.calculator.MortgageScheduleCalculator;
import creditpay.calculator.CalculatorRegistry;
import creditpay.io.ExcelCreditTermsReader;
import creditpay.io.ExcelPaymentWriter;
import creditpay.model.CreditTerms;
import creditpay.model.Payment;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class SwingApp {
    private static final Color SUCCESSFUL_TEXT_COLOR = new Color(0, 120, 0);
    private static final File DEFAULT_INPUT_DIR = new File("build/resources/main/excel");
    private static final File DEFAULT_OUTPUT_DIR = new File("build/output");

    private JFrame frame;
    private JLabel fileLabel;
    private JLabel statusLabel;
    private JScrollPane scrollPane;
    private DefaultTableModel tableModel;
    private JButton calculateButton;
    private JButton saveButton;
    private JPanel chartPanel;
    private CreditTerms creditTerms;
    private List<Payment> currentPayments;
    private ButtonGroup methodButtonsGroup;
    private Map<JRadioButton, MortgageScheduleCalculator> methodsMap;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SwingApp().createAndShowGUI());
    }

    private void createAndShowGUI() {
        frame = new JFrame("Mortgage Calculator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));

        // File selection
        JPanel topPanel = createTopPanel();
        frame.add(topPanel, BorderLayout.NORTH);

        // payment method + table + chart
        JPanel centerPanel = createCenterPanel();
        frame.add(centerPanel, BorderLayout.CENTER);

        // Status and buttons
        JPanel bottomPanel = createBottomPanel();
        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void initializeMethods(){
        methodsMap = new HashMap<JRadioButton, MortgageScheduleCalculator>();
        var result = CalculatorRegistry.discoverCalculators();

        methodButtonsGroup = new ButtonGroup();
        for (var method : result) {
            JRadioButton radioButton = new JRadioButton(method.getDisplayName());
            if(methodButtonsGroup.getButtonCount() == 0){
                radioButton.setSelected(true);
            }
            methodButtonsGroup.add(radioButton);
            methodsMap.put(radioButton, method);
        }
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Step 1: Load Credit Terms"));

        JButton browseButton = new JButton("Select Excel File");
        browseButton.addActionListener(this::browseFile);
        panel.add(browseButton);

        fileLabel = new JLabel("No file selected");
        fileLabel.setForeground(Color.GRAY);
        panel.add(fileLabel);

        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // method selection
        JPanel methodPanel = createMethodPanel();
        panel.add(methodPanel, BorderLayout.WEST);

        // Table and Chart
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));

        JPanel tablePanel = createTablePanel();
        rightPanel.add(tablePanel, BorderLayout.CENTER);

        chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (currentPayments != null && !currentPayments.isEmpty()) {
                    drawChart(g);
                }
            }
        };
        chartPanel.setBackground(Color.WHITE);
        chartPanel.setBorder(BorderFactory.createTitledBorder("Payment Schedule Chart"));
        chartPanel.setPreferredSize(new Dimension(0, 200));
        rightPanel.add(chartPanel, BorderLayout.SOUTH);

        panel.add(rightPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMethodPanel() {
        initializeMethods();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Step 2: Select Payment Method"));

        for (JRadioButton button : methodsMap.keySet()) {
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            panel.add(button);
        }

        panel.add(Box.createVerticalStrut(10));

        calculateButton = new JButton("Calculate Schedule");
        calculateButton.setEnabled(false);
        calculateButton.addActionListener(this::calculateSchedule);
        calculateButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        calculateButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        panel.add(calculateButton);

        panel.add(Box.createVerticalStrut(5));

        saveButton = new JButton("Save to Excel");
        saveButton.setEnabled(false);
        saveButton.addActionListener(this::saveToExcel);
        saveButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        panel.add(saveButton);

        panel.add(Box.createVerticalGlue());

        panel.setPreferredSize(new Dimension(200, 0));

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Step 3: Payment Schedule Table"));

        tableModel = new DefaultTableModel(
            new String[]{
                ExcelPaymentWriter.HEADER_NUMBER,
                ExcelPaymentWriter.HEADER_DAYS,
                ExcelPaymentWriter.HEADER_DATE,
                ExcelPaymentWriter.HEADER_TOTAL,
                ExcelPaymentWriter.HEADER_INTEREST,
                ExcelPaymentWriter.HEADER_PRINCIPAL,
                ExcelPaymentWriter.HEADER_REMAINING
            },
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable paymentTable = new JTable(tableModel);
        paymentTable.setFont(new Font("Monospaced", Font.PLAIN, 11));
        paymentTable.getTableHeader().setReorderingAllowed(false);
        paymentTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        JTableHeader header = paymentTable.getTableHeader();
        header.setDefaultRenderer(new WrappingHeaderRenderer());
        header.setPreferredSize(new Dimension(header.getWidth(), 60));

        scrollPane = new JScrollPane(paymentTable);
        scrollPane.setVisible(false);
        panel.add(scrollPane, BorderLayout.CENTER);

        hideTable();

        return panel;
    }

    private void hideTable() {
        scrollPane.setVisible(false);
    }

    private void showTable() {
        scrollPane.setVisible(true);
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createEtchedBorder());

        statusLabel = new JLabel("Status: Ready");
        statusLabel.setForeground(Color.BLUE);
        panel.add(statusLabel);

        return panel;
    }

    private void browseFile(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser(DEFAULT_INPUT_DIR);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files (*.xlsx)", "xlsx"));

        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            loadCreditTerms(selectedFile);
        }
    }

    private void loadCreditTerms(File file) {
        try {
            statusLabel.setText("Status: Loading file...");
            statusLabel.setForeground(Color.BLUE);
            frame.repaint();

            try (InputStream in = new FileInputStream(file)) {
                creditTerms = ExcelCreditTermsReader.read(in);
            }

            fileLabel.setText(file.getName() + " ✓");
            fileLabel.setForeground(SUCCESSFUL_TEXT_COLOR);
            calculateButton.setEnabled(true);
            saveButton.setEnabled(false);

            statusLabel.setText("Status: File loaded successfully. Principal: " +
                    formatCurrency(creditTerms.getPrincipal()) +
                    ", Term: " + creditTerms.getTermMonths() + " months, Rate: " +
                    creditTerms.getAnnualRatePercent() + "%");
            statusLabel.setForeground(SUCCESSFUL_TEXT_COLOR);
        } catch (Exception ex) {
            creditTerms = null;
            currentPayments = null;
            tableModel.setRowCount(0);
            hideTable();
            chartPanel.repaint();
            
            fileLabel.setText("Error reading file!");
            fileLabel.setForeground(Color.RED);
            calculateButton.setEnabled(false);
            saveButton.setEnabled(false);
            
            statusLabel.setText("Status: Error - " + ex.getMessage());
            statusLabel.setForeground(Color.RED);
            JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void calculateSchedule(ActionEvent e) {
        try {
            if (creditTerms == null) {
                JOptionPane.showMessageDialog(frame, "Please select a file first", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            statusLabel.setText("Status: Calculating...");
            statusLabel.setForeground(Color.BLUE);
            frame.repaint();

            MortgageScheduleCalculator calculator = null;
            for (Map.Entry<JRadioButton, MortgageScheduleCalculator> entry : methodsMap.entrySet()) {
                if (entry.getKey().isSelected()) {
                    calculator = entry.getValue();
                    break;
                }
            }

            if (calculator == null) {
                throw new Exception("Payment method is not implemented");
            }

            currentPayments = calculator.calculateSchedule(creditTerms);
            statusLabel.setText("Status: Payment schedule calculated (" + currentPayments.size() + " payments)");
            statusLabel.setForeground(SUCCESSFUL_TEXT_COLOR);

            updateTable();
            saveButton.setEnabled(true);
            chartPanel.repaint();
        } catch (Exception ex) {
            statusLabel.setText("Status: Error - " + ex.getMessage());
            statusLabel.setForeground(Color.RED);
            JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage(), "Calculation Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateTable() {
        tableModel.setRowCount(0);
        
        showTable();

        int rowNum = 1;
        for (Payment payment : currentPayments) {
            tableModel.addRow(new Object[]{
                    rowNum++,
                    payment.daysOfBorrowing,
                    payment.paymentDate,
                    formatCurrency(payment.totalPayment),
                    formatCurrency(payment.interest),
                    formatCurrency(payment.principalRepaid),
                    formatCurrency(payment.remainingDebt)
            });
        }
    }

    private void saveToExcel(ActionEvent e) {
        if (currentPayments == null || currentPayments.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No schedule to save", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser(DEFAULT_OUTPUT_DIR);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files (*.xlsx)", "xlsx"));
        fileChooser.setSelectedFile(new File("payment_schedule.xlsx"));

        int result = fileChooser.showSaveDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                statusLabel.setText("Status: Saving...");
                statusLabel.setForeground(Color.BLUE);
                frame.repaint();

                try (OutputStream out = new FileOutputStream(selectedFile)) {
                    ExcelPaymentWriter.write(currentPayments, out);
                }

                statusLabel.setText("Status: File saved successfully to " + selectedFile.getName());
                statusLabel.setForeground(SUCCESSFUL_TEXT_COLOR);
                JOptionPane.showMessageDialog(frame, "Schedule saved to " + selectedFile.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                statusLabel.setText("Status: Error - " + ex.getMessage());
                statusLabel.setForeground(Color.RED);
                JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void drawChart(Graphics g) {
        if (currentPayments == null || currentPayments.isEmpty()) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = chartPanel.getWidth();
        int height = chartPanel.getHeight();
        int padding = 40;
        int chartWidth = width - 2 * padding;
        int chartHeight = height - 2 * padding;

        BigDecimal maxPayment = currentPayments.stream()
                .map(p -> p.totalPayment)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ONE);
        double maxValue = maxPayment.doubleValue();

        g2d.setColor(Color.BLACK);
        g2d.drawLine(padding, height - padding, width - padding, height - padding); // X-axis
        g2d.drawLine(padding, padding, padding, height - padding); // Y-axis

        int numPayments = currentPayments.size();
        double barWidth = (double) chartWidth / numPayments;

        for (int i = 0; i < numPayments; i++) {
            Payment payment = currentPayments.get(i);
            double totalHeight = (payment.totalPayment.doubleValue() / maxValue) * chartHeight;
            double principalHeight = (payment.principalRepaid.doubleValue() / maxValue) * chartHeight;

            int x = padding + (int) (i * barWidth);
            int barHeightTotal = (int) totalHeight;

            int interestHeight = (int) ((payment.interest.doubleValue() / maxValue) * chartHeight);
            g2d.setColor(new Color(255, 0, 0, 150));
            g2d.fillRect(x, height - padding - barHeightTotal, (int) barWidth - 2, interestHeight);

            g2d.setColor(new Color(0, 0, 255, 150));
            g2d.fillRect(x, height - padding - barHeightTotal + interestHeight, (int) barWidth - 2, (int) principalHeight);
        }

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Principal", width - 150, 20);
        g2d.fillRect(width - 160, 8, 10, 10);
        g2d.setColor(new Color(0, 0, 255, 150));
        g2d.fillRect(width - 159, 9, 8, 8);

        g2d.setColor(Color.BLACK);
        g2d.drawString("Interest", width - 150, 40);
        g2d.fillRect(width - 160, 28, 10, 10);
        g2d.setColor(new Color(255, 0, 0, 150));
        g2d.fillRect(width - 159, 29, 8, 8);
    }

    private String formatCurrency(BigDecimal value) {
        NumberFormat format = NumberFormat.getInstance(Locale.US);
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(2);
        return format.format(value);
    }

    private static class WrappingHeaderRenderer extends JLabel implements TableCellRenderer {
        WrappingHeaderRenderer() {
            setOpaque(true);
            setBackground(UIManager.getColor("TableHeader.background"));
            setForeground(UIManager.getColor("TableHeader.foreground"));
            setFont(UIManager.getFont("TableHeader.font"));
            setHorizontalAlignment(JLabel.CENTER);
            setVerticalAlignment(JLabel.TOP);
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText("<html><div style='text-align: center;'>" + value + "</div></html>");
            return this;
        }
    }
}
