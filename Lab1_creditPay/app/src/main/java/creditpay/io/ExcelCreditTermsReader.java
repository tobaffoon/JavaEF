package creditpay.io;

import creditpay.model.CreditTerms;
import creditpay.model.DayOfMonthRangePeriod;
import creditpay.model.FixedLengthPeriod;
import creditpay.model.InterestPeriod;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.List;

/**
 * Утилита для чтения условий кредита из Excel-файлов разных простых форматов.
 */
public final class ExcelCreditTermsReader {
    private static int _headerNotFound = -1;

    private static final List<String> _principalKeys = List.of(
        "сумма кредита, руб",
        "сумма кредита",
        "сумма");
    private static final List<String> _termMonthsKeys = List.of(
        "срок, мес",
        "срок",
        "срок кредита, мес",
        "срок кредита");
    private static final List<String> _annualRateKeys = List.of(
        "процентная ставка",
        "ставка");
    private static final List<String> _paymentDayKeys = List.of(
        "платеж, день",
        "дата платежа",
        "платеж");
    private static final List<String> _interestPeriodKeys = List.of(
        "процентный период, дни",
        "период, дни",
        "процентный период",
        "период");
    private static final List<String> _startDateKeys = List.of(
        "дата предоставления",
        "дата предоставления кредита",
        "дата выдачи кредита",
        "дата выдачи");

    private ExcelCreditTermsReader() {}

    public static CreditTerms read(InputStream in) throws Exception {
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            return parseSheet(sheet);
        } catch (Exception e) {
            throw e;
        }
    }

    private static CreditTerms parseSheet(Sheet sheet) throws InvalidFormatException {
        DataFormatter formatter = new DataFormatter();

        BigDecimal principal = null;
        Integer termMonths = null;
        BigDecimal annualRate = null;
        InterestPeriod interestPeriod = null;
        LocalDate startDate = null;
        
        Row headerRow = sheet.getRow(0);
        Row valueRow = sheet.getRow(1);
        int colPrincipal = findColumn(headerRow, _principalKeys, formatter);
        int colTerm = findColumn(headerRow, _termMonthsKeys, formatter);
        int colRate = findColumn(headerRow, _annualRateKeys, formatter);
        int colStart = findColumn(headerRow, _startDateKeys, formatter);
        int colPeriod = findColumn(headerRow, _interestPeriodKeys, formatter);
        int colPaymentDay = findColumn(headerRow, _paymentDayKeys, formatter);

        if(colPrincipal == _headerNotFound || colTerm == _headerNotFound ||
            colRate == _headerNotFound || colStart == _headerNotFound ||
            colPeriod == _headerNotFound && colPaymentDay == _headerNotFound) {
                throw new InvalidFormatException("В таблице " + sheet.getSheetName() + " не найдены необходимые столбцы");
        }

        try {
            String rawValue = formatter.formatCellValue(valueRow.getCell(colPrincipal));
            principal = new BigDecimal(rawValue);
        } catch (NumberFormatException ex){
            throw new InvalidFormatException("Некорректное значение суммы кредита");
        }

        try {
            String rawValue = formatter.formatCellValue(valueRow.getCell(colTerm));
            termMonths = Integer.parseInt(rawValue);
        } catch (NumberFormatException ex){
            throw new InvalidFormatException("Некорректное значение срока кредита");
        }

        try {
            String rawValue = formatter.formatCellValue(valueRow.getCell(colRate));
            annualRate = new BigDecimal(rawValue);
        } catch (NumberFormatException ex){
            throw new InvalidFormatException("Некорректное значение процентной ставки");
        }

        try {
            String rawValue = formatter.formatCellValue(valueRow.getCell(colStart));
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            startDate = LocalDate.parse(rawValue, dateFormatter);
        } catch (DateTimeParseException ex){
            throw new InvalidFormatException("Некорректное значение даты предоставления кредита");
        }

        if(colPeriod != _headerNotFound) {
            try {
                String rawValue = formatter.formatCellValue(valueRow.getCell(colPeriod));
                int period = Integer.parseInt(rawValue);
                interestPeriod = new FixedLengthPeriod(period);
            } catch (NumberFormatException ex){
                throw new InvalidFormatException("Некорректное значение процентного периода");
            }
        }
        else{
            
            try {
                String rawValue = formatter.formatCellValue(valueRow.getCell(colPaymentDay));
                int paymentDay = Integer.parseInt(rawValue);
                interestPeriod = new DayOfMonthRangePeriod(paymentDay);
            } catch (NumberFormatException ex){
                throw new InvalidFormatException("Некорректное значение числа платежа");
            }
        }

        return new CreditTerms(principal, termMonths, annualRate, interestPeriod, startDate);
    }

    private static int findColumn(Row header, List<String> names, DataFormatter formatter) {
        for (int i = 0; i < header.getLastCellNum(); i++) {
            Cell cell = header.getCell(i);
            if (cell == null) continue;

            String value = formatter.formatCellValue(cell).toLowerCase();
            if (names.contains(value)) return i;
        }
        return _headerNotFound;
    }
}
