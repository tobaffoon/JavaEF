package creditpay.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to calculate mortgage repayment schedule for differentiated and annuity payments.
 */
public final class MortgageScheduleCalculator {
    public static List<Payment> differentiated(CreditTerms terms) {
        List<Payment> schedule = new ArrayList<>();
        BigDecimal principal = terms.getPrincipal();
        int months = terms.getTermMonths();
        BigDecimal rate = terms.getAnnualRatePercent().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        BigDecimal monthlyPrincipal = principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        LocalDate date = terms.getStartDate();
        BigDecimal remaining = principal;
        for (int m = 1; m <= months; m++) {
            BigDecimal interest = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal payment = monthlyPrincipal.add(interest);
            schedule.add(new Payment(date, monthlyPrincipal, interest, payment, remaining));
            remaining = remaining.subtract(monthlyPrincipal);
            date = date.plusMonths(1);
        }
        return schedule;
    }

    public static List<Payment> annuity(CreditTerms terms) {
        List<Payment> schedule = new ArrayList<>();
        BigDecimal principal = terms.getPrincipal();
        int months = terms.getTermMonths();
        BigDecimal rate = terms.getAnnualRatePercent().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        LocalDate date = terms.getStartDate();
        BigDecimal annuityCoeff = monthlyRate.add(BigDecimal.ONE).pow(months).subtract(BigDecimal.ONE);
        annuityCoeff = monthlyRate.multiply((monthlyRate.add(BigDecimal.ONE)).pow(months)).divide(annuityCoeff, 10, RoundingMode.HALF_UP);
        BigDecimal monthlyPayment = principal.multiply(annuityCoeff).setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = principal;
        for (int m = 1; m <= months; m++) {
            BigDecimal interest = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPart = monthlyPayment.subtract(interest).setScale(2, RoundingMode.HALF_UP);
            schedule.add(new Payment(date, principalPart, interest, monthlyPayment, remaining));
            remaining = remaining.subtract(principalPart);
            date = date.plusMonths(1);
        }
        return schedule;
    }

    public static class Payment {
        public final LocalDate date;
        public final BigDecimal principal;
        public final BigDecimal interest;
        public final BigDecimal total;
        public final BigDecimal remainingPrincipal;
        public Payment(LocalDate date, BigDecimal principal, BigDecimal interest, BigDecimal total, BigDecimal remainingPrincipal) {
            this.date = date;
            this.principal = principal;
            this.interest = interest;
            this.total = total;
            this.remainingPrincipal = remainingPrincipal;
        }
        @Override
        public String toString() {
            return String.format("%s | Principal: %.2f | Interest: %.2f | Total: %.2f | Remaining: %.2f", date, principal, interest, total, remainingPrincipal);
        }
    }
}
