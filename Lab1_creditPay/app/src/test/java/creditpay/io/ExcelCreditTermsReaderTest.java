package creditpay.io;

import creditpay.model.CreditTerms;
import creditpay.model.InterestPeriod;

import org.junit.jupiter.api.Test;

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

            assertInstanceOf(InterestPeriod.class, terms.getInterestPeriod());
            InterestPeriod period = (InterestPeriod)terms.getInterestPeriod();
            assertEquals(25, period.getPaymentDay());

            assertEquals(LocalDate.of(2022,9,22), terms.getStartDate());
        }
    }
}
