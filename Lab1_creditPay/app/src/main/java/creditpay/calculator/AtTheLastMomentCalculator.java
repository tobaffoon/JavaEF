package creditpay.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import creditpay.model.Payment;

public class AtTheLastMomentCalculator extends MortgageScheduleCalculator {
    private static final String _displayName = "At the last moment";

    @Override
    public List<creditpay.model.Payment> calculateSchedule(creditpay.model.CreditTerms terms) {
        
        List<Payment> schedule = new ArrayList<>();
        CalculationParams params = initializeCalculation(terms);
        
        LocalDate previousAccrualDate = params.startDate;
        BigDecimal remaining = params.principal;
        int daysOfBorrowing = 0;
        
        for (int m = 0; m < params.months - 1; m++) {
            LocalDate currentAccrualDate = params.interestPeriod.nextAccrualDate(previousAccrualDate);
            daysOfBorrowing += (int) ChronoUnit.DAYS.between(previousAccrualDate, currentAccrualDate);
            
            BigDecimal interest = remaining.multiply(params.monthlyRate);
            remaining = remaining.add(interest);
            
            schedule.add(new Payment(daysOfBorrowing, currentAccrualDate, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, remaining));
            
            previousAccrualDate = currentAccrualDate;
        }

        BigDecimal interest = remaining.multiply(params.monthlyRate);
        remaining = remaining.add(interest);
        schedule.add(new Payment(
            (int) ChronoUnit.DAYS.between(previousAccrualDate, params.interestPeriod.nextAccrualDate(previousAccrualDate)),
            params.interestPeriod.nextAccrualDate(previousAccrualDate),
            remaining,
            remaining.subtract(params.principal),
            params.principal,
            BigDecimal.ZERO
        ));

        return schedule;
    }

    @Override
    public String getDisplayName() {
        return _displayName;
    }
    
}
