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
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ExcelCreditTermsReaderTest {

    @Test
    public void readKeyValueFormat() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("sheet1");
            int r = 0;
            Row row = s.createRow(r++); row.createCell(0).setCellValue("Сумма кредита"); row.createCell(1).setCellValue("9 200 000.00");
            row = s.createRow(r++); row.createCell(0).setCellValue("Срок"); row.createCell(1).setCellValue("276");
            row = s.createRow(r++); row.createCell(0).setCellValue("Процентная ставка"); row.createCell(1).setCellValue("7.45");
            row = s.createRow(r++); row.createCell(0).setCellValue("Платеж"); row.createCell(1).setCellValue("25");
            row = s.createRow(r++); row.createCell(0).setCellValue("Дата предоставления"); row.createCell(1).setCellValue("22.09.2022");
            row = s.createRow(r++); row.createCell(0).setCellValue("Процентный период"); row.createCell(1).setCellValue("26-25");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            try (InputStream in = new ByteArrayInputStream(out.toByteArray())) {
                CreditTerms terms = ExcelCreditTermsReader.read(in);
                assertNotNull(terms);
                assertEquals(new BigDecimal("9200000.00"), terms.getPrincipal());
                assertEquals(276, terms.getTermMonths());
                assertEquals(new BigDecimal("7.45"), terms.getAnnualRatePercent());
                assertEquals(25, terms.getPaymentDayOfMonth());
                assertEquals(LocalDate.of(2022,9,22), terms.getStartDate());
                assertTrue(terms.getInterestPeriod() instanceof DayOfMonthRangePeriod);
            }
        }
    }

    @Test
    public void readTabularFormat() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("sheet1");
            Row header = s.createRow(0);
            header.createCell(0).setCellValue("Сумма");
            header.createCell(1).setCellValue("Срок");
            header.createCell(2).setCellValue("Ставка");
            header.createCell(3).setCellValue("Дата предоставления");
            header.createCell(4).setCellValue("Период");
            Row row = s.createRow(1);
            row.createCell(0).setCellValue("9200000.00");
            row.createCell(1).setCellValue("276");
            row.createCell(2).setCellValue("7.45");
            row.createCell(3).setCellValue("22.09.2022");
            row.createCell(4).setCellValue("30d");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            try (InputStream in = new ByteArrayInputStream(out.toByteArray())) {
                CreditTerms terms = ExcelCreditTermsReader.read(in);
                assertNotNull(terms);
                assertEquals(new BigDecimal("9200000.00"), terms.getPrincipal());
                assertEquals(276, terms.getTermMonths());
                assertEquals(new BigDecimal("7.45"), terms.getAnnualRatePercent());
                assertTrue(terms.getInterestPeriod() instanceof FixedLengthPeriod);
            }
        }
    }
}
