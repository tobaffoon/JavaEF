package creditpay.io;

import creditpay.MortgageScheduleCalculator;
import creditpay.model.CreditTerms;
import creditpay.model.InterestPeriod;
import creditpay.model.Payment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExcelPaymentWriterTest {
    private static final Path OUTPUT_DIR = Paths.get("build/output");

    @BeforeAll
    public static void setUp() throws Exception {
        // Create output directory if it doesn't exist
        Files.createDirectories(OUTPUT_DIR);
    }

    @Test
    public void writeDifferentiatedPayments() throws Exception {
        // Create credit terms
        CreditTerms terms = new CreditTerms(
            new BigDecimal("1000000.00"),
            100,
            new BigDecimal("7.45"),
            new InterestPeriod(25),
            LocalDate.of(2024, 1, 25)
        );

        // Calculate using differentiated method
        List<Payment> payments = MortgageScheduleCalculator.differentiated(terms);

        // Write to Excel
        Path outputFile = OUTPUT_DIR.resolve("differentiated_payments.xlsx");
        try (OutputStream out = new FileOutputStream(outputFile.toFile())) {
            ExcelPaymentWriter.write(payments, out);
        }
    }

    @Test
    public void writeAnnuityPayments() throws Exception {
        // Create credit terms
        CreditTerms terms = new CreditTerms(
            new BigDecimal("500000.00"),
            60,
            new BigDecimal("7.45"),
            new InterestPeriod(25),
            LocalDate.of(2024, 1, 25)
        );

        // Calculate using annuity method
        List<Payment> payments = MortgageScheduleCalculator.annuity(terms);

        // Write to Excel
        Path outputFile = OUTPUT_DIR.resolve("annuity_payments.xlsx");
        try (OutputStream out = new FileOutputStream(outputFile.toFile())) {
            ExcelPaymentWriter.write(payments, out);
        }
    }

    @Test
    public void writeEmptyPaymentList() throws Exception {
        // Test writing empty list
        List<Payment> emptyPayments = new ArrayList<>();

        Path outputFile = OUTPUT_DIR.resolve("empty_payments.xlsx");
        try (OutputStream out = new FileOutputStream(outputFile.toFile())) {
            ExcelPaymentWriter.write(emptyPayments, out);
        }
    }

    @Test
    public void writeSinglePayment() throws Exception {
        // Test writing single payment
        List<Payment> payments = new ArrayList<>();
        payments.add(new Payment(
            30,
            LocalDate.of(2024, 1, 25),
            new BigDecimal("15000.00"),
            new BigDecimal("5000.00"),
            new BigDecimal("10000.00"),
            new BigDecimal("490000.00")
        ));

        Path outputFile = OUTPUT_DIR.resolve("single_payment.xlsx");
        try (OutputStream out = new FileOutputStream(outputFile.toFile())) {
            ExcelPaymentWriter.write(payments, out);
        }
    }
}
