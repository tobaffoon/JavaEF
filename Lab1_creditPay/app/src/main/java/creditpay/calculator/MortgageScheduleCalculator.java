package creditpay.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import creditpay.model.InterestPeriod;
import creditpay.model.CreditTerms;
import creditpay.model.Payment;

public abstract class MortgageScheduleCalculator {    
    protected static final int _calculationScale = 10;

    protected static CalculationParams initializeCalculation(CreditTerms terms) {
        BigDecimal principal = terms.getPrincipal();
        int months = terms.getTermMonths();
        BigDecimal rate = terms.getAnnualRatePercent().divide(BigDecimal.valueOf(100), _calculationScale, RoundingMode.HALF_UP);
        BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(12), _calculationScale, RoundingMode.HALF_UP);
        return new CalculationParams(principal, months, monthlyRate, terms.getStartDate(), terms.getInterestPeriod());
    }
    
    public abstract List<Payment> calculateSchedule(CreditTerms terms);
    
    protected static class CalculationParams {
        public final BigDecimal principal;
        public final int months;
        public final BigDecimal monthlyRate;
        public final LocalDate startDate;
        public final InterestPeriod interestPeriod;
        
        CalculationParams(BigDecimal principal, int months, BigDecimal monthlyRate, LocalDate startDate, creditpay.model.InterestPeriod interestPeriod) {
            this.principal = principal;
            this.months = months;
            this.monthlyRate = monthlyRate;
            this.startDate = startDate;
            this.interestPeriod = interestPeriod;
        }
    }
}