package creditpay.io;

import creditpay.model.Payment;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Утилита для записи графика платежей в Excel-файл.
 */
public final class ExcelPaymentWriter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private ExcelPaymentWriter() {}

    /**
     * Записывает график платежей в Excel-файл.
     */
    public static void write(List<Payment> payments, OutputStream out) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("График платежей");
            CreationHelper helper = wb.getCreationHelper();

            // Создание стиля для заголовка
            var headerFont = wb.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(headerFont);

            Row mainHeaderRow = sheet.createRow(0);
            var cell0 = mainHeaderRow.createCell(0);
            cell0.setCellValue("№ п/п");
            cell0.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 1, 0, 0));

            var cell1 = mainHeaderRow.createCell(1);
            cell1.setCellValue("Кол-во дней пользования заемными средствами");
            cell1.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 1, 1, 1));

            var cell2 = mainHeaderRow.createCell(2);
            cell2.setCellValue("Дата платежа");
            cell2.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 1, 2, 2));

            var cell3 = mainHeaderRow.createCell(3);
            cell3.setCellValue("Общая сумма платежа");
            cell3.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 1, 3, 3));

            var cellMerged = mainHeaderRow.createCell(4);
            cellMerged.setCellValue("В том числе");
            cellMerged.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 4, 6));

            Row subHeaderRow = sheet.createRow(1);
            var subCell4 = subHeaderRow.createCell(4);
            subCell4.setCellValue("Сумма процентов");
            subCell4.setCellStyle(headerStyle);

            var subCell5 = subHeaderRow.createCell(5);
            subCell5.setCellValue("Сумма погашаемого долга");
            subCell5.setCellStyle(headerStyle);

            var subCell6 = subHeaderRow.createCell(6);
            subCell6.setCellValue("Остаток задолженности");
            subCell6.setCellStyle(headerStyle);

            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(helper.createDataFormat().getFormat("dd.MM.yyyy"));

            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.setDataFormat(helper.createDataFormat().getFormat("#,##0.00"));

            int rowNum = 2;
            for (Payment payment : payments) {
                Row row = sheet.createRow(rowNum);

                row.createCell(0).setCellValue(rowNum - 1);

                row.createCell(1).setCellValue(payment.daysOfBorrowing);

                var dateCell = row.createCell(2);
                dateCell.setCellValue(payment.paymentDate.format(DATE_FORMATTER));
                dateCell.setCellStyle(dateStyle);

                var totalCell = row.createCell(3);
                totalCell.setCellValue(payment.totalPayment.doubleValue());
                totalCell.setCellStyle(moneyStyle);

                var interestCell = row.createCell(4);
                interestCell.setCellValue(payment.interest.doubleValue());
                interestCell.setCellStyle(moneyStyle);

                var principalCell = row.createCell(5);
                principalCell.setCellValue(payment.principalRepaid.doubleValue());
                principalCell.setCellStyle(moneyStyle);

                var remainingCell = row.createCell(6);
                remainingCell.setCellValue(payment.remainingDebt.doubleValue());
                remainingCell.setCellStyle(moneyStyle);

                rowNum++;
            }

            // Автоматический размер колонок
            for (int i = 0; i < 7; i++) {
                sheet.autoSizeColumn(i);
            }

            // Запись в поток
            wb.write(out);
        }
    }
}
