package creditpay;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import creditpay.model.InterestPeriod;
import creditpay.model.CreditTerms;
import creditpay.model.Payment;

public final class MortgageScheduleCalculator {    
    private static final int _calculationScale = 10;

    private static CalculationParams initializeCalculation(CreditTerms terms) {
        BigDecimal principal = terms.getPrincipal();
        int months = terms.getTermMonths();
        BigDecimal rate = terms.getAnnualRatePercent().divide(BigDecimal.valueOf(100), _calculationScale, RoundingMode.HALF_UP);
        BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(12), _calculationScale, RoundingMode.HALF_UP);
        return new CalculationParams(principal, months, monthlyRate, terms.getStartDate(), terms.getInterestPeriod());
    }
    
    public static List<Payment> differentiated(CreditTerms terms) {
        List<Payment> schedule = new ArrayList<>();
        CalculationParams params = initializeCalculation(terms);
        
        BigDecimal monthlyPrincipal = params.principal.divide(BigDecimal.valueOf(params.months), _calculationScale, RoundingMode.HALF_UP);
        LocalDate previousAccrualDate = params.startDate;
        BigDecimal remaining = params.principal;
        int daysOfBorrowing = 0;
        
        for (int m = 0; m < params.months; m++) {
            LocalDate currentAccrualDate = params.interestPeriod.nextAccrualDate(previousAccrualDate);
            daysOfBorrowing += (int) ChronoUnit.DAYS.between(previousAccrualDate, currentAccrualDate);
            
            BigDecimal interest = remaining.multiply(params.monthlyRate);
            BigDecimal totalPayment = monthlyPrincipal.add(interest);
            BigDecimal newRemaining = remaining.subtract(monthlyPrincipal);
            
            schedule.add(new Payment(daysOfBorrowing, currentAccrualDate, totalPayment, interest, monthlyPrincipal, newRemaining));
            
            remaining = newRemaining;
            previousAccrualDate = currentAccrualDate;
        }
        return schedule;
    }

    public static List<Payment> annuity(CreditTerms terms) {
        List<Payment> schedule = new ArrayList<>();
        CalculationParams params = initializeCalculation(terms);
        
        LocalDate previousAccrualDate = params.startDate;
        
        BigDecimal annuityCoeff = params.monthlyRate.add(BigDecimal.ONE).pow(params.months).subtract(BigDecimal.ONE);
        annuityCoeff = params.monthlyRate.multiply((params.monthlyRate.add(BigDecimal.ONE)).pow(params.months)).divide(annuityCoeff, _calculationScale, RoundingMode.HALF_UP);
        BigDecimal monthlyPayment = params.principal.multiply(annuityCoeff);
        BigDecimal remaining = params.principal;
        int daysOfBorrowing = 0;
        
        for (int m = 0; m < params.months; m++) {
            LocalDate currentAccrualDate = params.interestPeriod.nextAccrualDate(previousAccrualDate);
            daysOfBorrowing += (int) ChronoUnit.DAYS.between(previousAccrualDate, currentAccrualDate);
            
            BigDecimal interest = remaining.multiply(params.monthlyRate);
            
            BigDecimal currentPayment;
            BigDecimal principalPart;
            BigDecimal newRemaining;
            if (m == params.months - 1) {
                newRemaining = BigDecimal.ZERO;
                currentPayment = remaining.add(interest);
                principalPart = remaining;
            } else {
                currentPayment = monthlyPayment;
                principalPart = monthlyPayment.subtract(interest);
                newRemaining = remaining.subtract(principalPart);
            }
            
            schedule.add(new Payment(daysOfBorrowing, currentAccrualDate, currentPayment, interest, principalPart, newRemaining));
            
            remaining = newRemaining;
            previousAccrualDate = currentAccrualDate;
        }
        return schedule;
    }
    
    private static class CalculationParams {
        private final BigDecimal principal;
        private final int months;
        private final BigDecimal monthlyRate;
        private final LocalDate startDate;
        private final InterestPeriod interestPeriod;
        
        CalculationParams(BigDecimal principal, int months, BigDecimal monthlyRate, LocalDate startDate, creditpay.model.InterestPeriod interestPeriod) {
            this.principal = principal;
            this.months = months;
            this.monthlyRate = monthlyRate;
            this.startDate = startDate;
            this.interestPeriod = interestPeriod;
        }
    }
}