package creditpay.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import creditpay.model.CreditTerms;
import creditpay.model.Payment;

public class AnnuityCalculator extends MortgageScheduleCalculator {
    
    public List<Payment> calculateSchedule(CreditTerms terms) {
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
}
