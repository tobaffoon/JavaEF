package creditpay.io;

import creditpay.model.CreditTerms;
import creditpay.model.DayOfMonthRangePeriod;
import creditpay.model.FixedLengthPeriod;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ExcelCreditTermsReaderTest {

    @Test
    public void readDayOfMonthTerms() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("excel/exampleData.xlsx")) {
            CreditTerms terms = ExcelCreditTermsReader.read(in);
            assertNotNull(terms);
            assertEquals(0, terms.getPrincipal().compareTo(new BigDecimal("9200000.00")));
            assertEquals(276, terms.getTermMonths());
            assertEquals(0, terms.getAnnualRatePercent().compareTo(new BigDecimal("7.45")));

            assertInstanceOf(DayOfMonthRangePeriod.class, terms.getInterestPeriod());
            DayOfMonthRangePeriod period = (DayOfMonthRangePeriod)terms.getInterestPeriod();
            assertEquals(25, period.getPaymentDay());

            assertEquals(LocalDate.of(2022,9,22), terms.getStartDate());
            assertTrue(terms.getInterestPeriod() instanceof DayOfMonthRangePeriod);
        }
    }

    @Test
    public void readFixedLengthTerms() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("excel/exampleDataFixedLength.xlsx")) {
            CreditTerms terms = ExcelCreditTermsReader.read(in);
            assertNotNull(terms);
            assertEquals(0, terms.getPrincipal().compareTo(new BigDecimal("9200000.00")));
            assertEquals(276, terms.getTermMonths());
            assertEquals(0, terms.getAnnualRatePercent().compareTo(new BigDecimal("7.45")));

            assertInstanceOf(FixedLengthPeriod.class, terms.getInterestPeriod());
            FixedLengthPeriod period = (FixedLengthPeriod)terms.getInterestPeriod();
            assertEquals(30, period.getLengthInDays());

            assertEquals(LocalDate.of(2022,9,22), terms.getStartDate());
            assertTrue(terms.getInterestPeriod() instanceof FixedLengthPeriod);
        }
    }
}
