package creditpay.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import creditpay.model.CreditTerms;
import creditpay.model.Payment;

public class DifferentiatedCalculator extends MortgageScheduleCalculator {
    private static final String _displayName = "Differentiated";

    @Override
    public List<Payment> calculateSchedule(CreditTerms terms) {
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

    @Override
    public String getDisplayName() {
        return _displayName;
    }
}
