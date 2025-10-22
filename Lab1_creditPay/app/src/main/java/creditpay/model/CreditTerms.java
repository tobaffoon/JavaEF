package creditpay.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Класс, представляющий условия кредита.
 */
public final class CreditTerms {
    private BigDecimal principal; // сумма кредита в рублях
    private int termMonths; // срок в месяцах
    private BigDecimal annualRatePercent; // процентная ставка в % годовых
    private InterestPeriod interestPeriod; // произвольный период начисления процентов
    private int paymentDayOfMonth; // число платежа (25)
    private LocalDate startDate; // дата предоставления кредита

    private CreditTerms(BigDecimal principal, int termMonths, BigDecimal annualRatePercent,
                        InterestPeriod interestPeriod, int paymentDayOfMonth, LocalDate startDate) {
        this.principal = principal;
        this.termMonths = termMonths;
        this.annualRatePercent = annualRatePercent;
        this.interestPeriod = interestPeriod;
        this.paymentDayOfMonth = paymentDayOfMonth;
        this.startDate = startDate;
    }

    /**
     * Public factory to create CreditTerms from external utilities.
     */
    public static CreditTerms of(BigDecimal principal, int termMonths, BigDecimal annualRatePercent,
                                 InterestPeriod interestPeriod, int paymentDayOfMonth, LocalDate startDate) {
        return new CreditTerms(principal, termMonths, annualRatePercent, interestPeriod, paymentDayOfMonth, startDate);
    }

    public static CreditTerms sample() {
        BigDecimal principal = new BigDecimal("9200000.00");
        int termMonths = 276;
        BigDecimal annualRatePercent = new BigDecimal("7.45");
        int paymentDayOfMonth = 25;
        LocalDate startDate = LocalDate.of(2022, 9, 22);
        // пример: месячный период с 26 по 25
        InterestPeriod interestPeriod = new DayOfMonthRangePeriod(26, 25);
        return new CreditTerms(principal, termMonths, annualRatePercent, interestPeriod, paymentDayOfMonth, startDate);
    }

    public BigDecimal getPrincipal() {
        return principal;
    }

    public int getTermMonths() {
        return termMonths;
    }

    public BigDecimal getAnnualRatePercent() {
        return annualRatePercent;
    }

    public InterestPeriod getInterestPeriod() {
        return interestPeriod;
    }

    public int getPaymentDayOfMonth() {
        return paymentDayOfMonth;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    @Override
    public String toString() {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        StringBuilder sb = new StringBuilder();
        sb.append("Сумма: ").append(principal).append(" руб., ");
        sb.append("Срок: ").append(termMonths).append(" мес., ");
        sb.append("Ставка: ").append(annualRatePercent).append("% годовых, ");
        sb.append("Платеж: ").append(paymentDayOfMonth).append(" число, ");
        sb.append("Процентный период: ").append(interestPeriod).append(", ");
        sb.append("Дата предоставления: ").append(startDate.format(df));
        return sb.toString();
    }
}
