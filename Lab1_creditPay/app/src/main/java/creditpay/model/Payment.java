package creditpay.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Payment {
    /*
     * Количество дней пользования заемными средствами.
     */
    public final int daysOfBorrowing;
    /*
     * Дата платежа.
     */
    public final LocalDate paymentDate;
    /*
     * Общая сумма платежа.
     */ 
    public final BigDecimal totalPayment;
    /*
     * Сумма процентов.
     */
    public final BigDecimal interest;
    /*
     * Сумма погашаемого долга.
     */
    public final BigDecimal principalRepaid;
    /*
     * Остаток задолженности.
     */
    public final BigDecimal remainingDebt;
    
    public Payment(int daysOfBorrowing, LocalDate paymentDate, BigDecimal totalPayment, 
                   BigDecimal interest, BigDecimal principalRepaid, BigDecimal remainingDebt) {
        this.daysOfBorrowing = daysOfBorrowing;
        this.paymentDate = paymentDate;
        this.totalPayment = totalPayment;
        this.interest = interest;
        this.principalRepaid = principalRepaid;
        this.remainingDebt = remainingDebt;
    }
}