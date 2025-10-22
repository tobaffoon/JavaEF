package creditpay.io;

import creditpay.model.CreditTerms;
import creditpay.model.DayOfMonthRangePeriod;
import creditpay.model.FixedLengthPeriod;
import creditpay.model.InterestPeriod;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

/**
 * Утилита для чтения условий кредита из Excel-файлов разных простых форматов.
 *
 * Поддерживает варианты, где поля находятся в строках вида "Ключ" | "Значение"
 * и варианты табличного вида (имя столбца).
 *
 * Это минимальная реализация, ориентированная на локальные тесты; можно расширить
 * под конкретные шаблоны Excel.
 */
public final class ExcelCreditTermsReader {
    private ExcelCreditTermsReader() {}

    public static CreditTerms read(InputStream in) throws IOException {
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            return parseSheet(sheet);
        } catch (Exception e) {
            throw new IOException("Failed to read Excel", e);
        }
    }

    private static CreditTerms parseSheet(Sheet sheet) {
        BigDecimal principal = null;
        Integer termMonths = null;
        BigDecimal annualRate = null;
        InterestPeriod interestPeriod = null;
        Integer paymentDay = null;
        LocalDate startDate = null;

        DateTimeFormatter df = DateTimeFormatter.ofPattern("d.MM.yyyy");

        // try row-by-row key|value first
        for (Row row : sheet) {
            Cell keyCell = row.getCell(0);
            Cell valCell = row.getCell(1);
            if (keyCell == null || valCell == null) continue;
            String key = cellAsString(keyCell).trim().toLowerCase();
            String val = cellAsString(valCell).trim();
            if (key.isEmpty() || val.isEmpty()) continue;

            switch (key) {
                case "сумма кредита":
                case "сумма":
                    if (val.matches(".*\\d.*")) {
                        principal = new BigDecimal(val.replaceAll("[^0-9,\\.]","").replace(',', '.'));
                    }
                    break;
                case "срок (мес)":
                case "срок":
                case "срок кредита":
                    if (val.matches(".*\\d.*")) {
                        termMonths = Integer.parseInt(val.replaceAll("[^0-9]",""));
                    }
                    break;
                case "процентная ставка":
                case "ставка":
                    if (val.matches(".*\\d.*")) {
                        annualRate = new BigDecimal(val.replace(',', '.'));
                    }
                    break;
                case "платеж (день)":
                case "дата платежа":
                case "платеж":
                    if (val.matches(".*\\d.*")) {
                        paymentDay = Integer.parseInt(val.replaceAll("[^0-9]",""));
                    }
                    break;
                case "дата предоставления":
                case "дата выдачи":
                    if (val.matches(".*\\d.*")) {
                        try {
                            startDate = LocalDate.parse(val, df);
                        } catch (Exception ex) {
                            // ignore parse errors here
                        }
                    }
                    break;
                case "процентный период":
                case "период":
                    // accept two simple syntaxes: "26-25" or "30d" or "30d+2"
                    if (val.contains("-")) {
                        String[] parts = val.split("-", 2);
                        int s = Integer.parseInt(parts[0].trim());
                        int e = Integer.parseInt(parts[1].trim());
                        interestPeriod = new DayOfMonthRangePeriod(s, e);
                    } else if (val.toLowerCase().endsWith("d")) {
                        String num = val.substring(0, val.length()-1).trim();
                        int days = Integer.parseInt(num.replaceAll("[^0-9]",""));
                        interestPeriod = new FixedLengthPeriod(days, 0);
                    } else if (val.matches("\\d+d\\+\\d+")) {
                        String[] parts = val.split("d\\+");
                        int days = Integer.parseInt(parts[0]);
                        int offs = Integer.parseInt(parts[1]);
                        interestPeriod = new FixedLengthPeriod(days, offs);
                    }
                    break;
                default:
                    // ignore unknown keys
            }
        }

        // fallback: try header-based parsing if missing fields
        if (principal == null || termMonths == null || annualRate == null) {
            Iterator<Row> it = sheet.iterator();
            if (it.hasNext()) {
                Row header = it.next();
                int cols = header.getLastCellNum();
                // naive header parsing
                int colPrincipal = findColumn(header, "сумма");
                int colTerm = findColumn(header, "срок");
                int colRate = findColumn(header, "ставка");
                int colStart = findColumn(header, "дата предоставления");
                int colPeriod = findColumn(header, "период");
                int colPayment = findColumn(header, "платеж");
                if (colPrincipal >= 0 || colTerm >= 0 || colRate >= 0) {
                    while (it.hasNext()) {
                        Row r = it.next();
                        if (colPrincipal >= 0 && principal == null) principal = cellAsBigDecimal(r.getCell(colPrincipal));
                        if (colTerm >= 0 && termMonths == null) termMonths = cellAsInt(r.getCell(colTerm));
                        if (colRate >= 0 && annualRate == null) annualRate = cellAsBigDecimal(r.getCell(colRate));
                        if (colStart >= 0 && startDate == null) startDate = cellAsDate(r.getCell(colStart), df);
                        if (colPeriod >= 0 && interestPeriod == null) {
                            String v = cellAsString(r.getCell(colPeriod)).trim();
                            if (v.contains("-")) {
                                String[] parts = v.split("-", 2);
                                int s = Integer.parseInt(parts[0].trim());
                                int e = Integer.parseInt(parts[1].trim());
                                interestPeriod = new DayOfMonthRangePeriod(s, e);
                            } else if (v.toLowerCase().endsWith("d")) {
                                String num = v.substring(0, v.length() - 1).trim();
                                if (num.matches(".*\\d.*")) {
                                    int days = Integer.parseInt(num.replaceAll("[^0-9]", ""));
                                    interestPeriod = new FixedLengthPeriod(days, 0);
                                }
                            } else if (v.matches("\\d+d\\+\\d+")) {
                                String[] parts = v.split("d\\+");
                                int days = Integer.parseInt(parts[0]);
                                int offs = Integer.parseInt(parts[1]);
                                interestPeriod = new FixedLengthPeriod(days, offs);
                            }
                        }
                        if (colPayment >= 0 && paymentDay == null) paymentDay = cellAsInt(r.getCell(colPayment));
                    }
                }
            }
        }

        // minimal validation and defaults
        if (principal == null) principal = BigDecimal.ZERO;
        if (termMonths == null) termMonths = 0;
        if (annualRate == null) annualRate = BigDecimal.ZERO;
        if (interestPeriod == null) interestPeriod = new DayOfMonthRangePeriod(26, 25);
        if (paymentDay == null) paymentDay = 25;
        if (startDate == null) startDate = LocalDate.now();

        return CreditTerms.of(principal, termMonths, annualRate, interestPeriod, paymentDay, startDate);
    }

    private static String cellAsString(Cell c) {
        if (c == null) return "";
        switch (c.getCellType()) {
            case STRING: return c.getStringCellValue();
            case NUMERIC: return String.valueOf(c.getNumericCellValue());
            case BOOLEAN: return String.valueOf(c.getBooleanCellValue());
            default: return "";
        }
    }

    private static BigDecimal cellAsBigDecimal(Cell c) {
        String s = cellAsString(c).replaceAll("[^0-9,\\.]","" ).replace(',', '.');
        if (s.isEmpty()) return null;
        return new BigDecimal(s);
    }

    private static Integer cellAsInt(Cell c) {
        String s = cellAsString(c).replaceAll("[^0-9]","" );
        if (s.isEmpty()) return null;
        return Integer.parseInt(s);
    }

    private static LocalDate cellAsDate(Cell c, DateTimeFormatter df) {
        String s = cellAsString(c).trim();
        if (s.isEmpty()) return null;
        try { return LocalDate.parse(s, df); } catch (Exception ex) { return null; }
    }

    private static int findColumn(Row header, String name) {
        for (int i = 0; i < header.getLastCellNum(); i++) {
            Cell c = header.getCell(i);
            if (c == null) continue;
            String v = cellAsString(c).toLowerCase();
            if (v.contains(name)) return i;
        }
        return -1;
    }
}
