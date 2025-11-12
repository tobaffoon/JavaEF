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
    private static final int DAYS_IN_YEAR = 365; // для конвертации годовой ставки в дневнуюё

    private static CalculationParams initializeCalculation(CreditTerms terms) {
        BigDecimal principal = terms.getPrincipal();
        int months = terms.getTermMonths();
        BigDecimal rate = terms.getAnnualRatePercent().divide(BigDecimal.valueOf(100), _calculationScale, RoundingMode.HALF_UP);
        // Вычисляем дневную ставку, чтобы учесть период с фиксированной длиной в днях
        BigDecimal dailyRate = rate.divide(BigDecimal.valueOf(DAYS_IN_YEAR), _calculationScale, RoundingMode.HALF_UP);
        return new CalculationParams(principal, months, dailyRate, terms.getStartDate(), terms.getInterestPeriod());
    }
    
    public static List<Payment> differentiated(CreditTerms terms) {
        List<Payment> schedule = new ArrayList<>();
        CalculationParams params = initializeCalculation(terms);
        
        LocalDate previousAccrualDate = params.startDate;
        BigDecimal remaining = params.principal;
        
        List<LocalDate> accrualDates = calculateAccrualDates(params.startDate, params.interestPeriod, params.months);
        int totalPeriods = accrualDates.size();

        BigDecimal principalPerPeriod = params.principal.divide(BigDecimal.valueOf(totalPeriods), _calculationScale, RoundingMode.HALF_UP);
        
        for(int i = 0; i < totalPeriods; i++) {
            LocalDate currentAccrualDate = accrualDates.get(i);
            int daysOfBorrowing = (int) ChronoUnit.DAYS.between(previousAccrualDate, currentAccrualDate);
            
            BigDecimal principalPayment;
            BigDecimal interest;
            
            // В последний период выплачиваем остаток основного долга
            if (i == accrualDates.size() - 1) {
                principalPayment = remaining;
                interest = BigDecimal.ZERO;
            }
            else{
                interest = remaining.multiply(params.dailyRate).multiply(BigDecimal.valueOf(daysOfBorrowing));
                principalPayment = principalPerPeriod;
            }
            BigDecimal totalPayment = principalPayment.add(interest);
            
            BigDecimal newRemaining = remaining.subtract(totalPayment);
            
            schedule.add(new Payment(daysOfBorrowing, currentAccrualDate, totalPayment, interest, totalPayment, newRemaining));
            
            remaining = newRemaining;
            previousAccrualDate = currentAccrualDate;
        }
        return schedule;
    }

    public static List<Payment> annuity(CreditTerms terms) {
        List<Payment> schedule = new ArrayList<>();
        CalculationParams params = initializeCalculation(terms);
        
        LocalDate previousAccrualDate = params.startDate;
        BigDecimal remaining = params.principal;
        
        List<LocalDate> accrualDates = calculateAccrualDates(params.startDate, params.interestPeriod, params.months);
        int totalPeriods = accrualDates.size();
        
        BigDecimal annuityPayment = calculateAnnuityPayment(params.principal, params.dailyRate, accrualDates);
        
        for(int i = 0; i < totalPeriods; i++) {
            LocalDate currentAccrualDate = accrualDates.get(i);
            int daysOfBorrowing = (int) ChronoUnit.DAYS.between(previousAccrualDate, currentAccrualDate);
            
            BigDecimal totalPayment;
            BigDecimal interest;
            // В последний период выплачиваем остаток основного долга
            if (i == accrualDates.size() - 1) {
                totalPayment = remaining;
                interest = BigDecimal.ZERO;
            }
            else{
                totalPayment = annuityPayment;
                interest = remaining.multiply(params.dailyRate).multiply(BigDecimal.valueOf(daysOfBorrowing));
            }
            
            BigDecimal newRemaining = remaining.add(interest).subtract(totalPayment);
            
            schedule.add(new Payment(daysOfBorrowing, currentAccrualDate, totalPayment, interest, totalPayment, newRemaining));
            
            remaining = newRemaining;
            previousAccrualDate = currentAccrualDate;
        }
        return schedule;
    }
    
    private static List<LocalDate> calculateAccrualDates(LocalDate startDate, InterestPeriod interestPeriod, int termMonths) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate currentDate = startDate;
        LocalDate termLimit = startDate.plusMonths(termMonths);
        
        while (currentDate.isBefore(termLimit)) {
            LocalDate nextDate = interestPeriod.nextAccrualDate(currentDate);
            if (nextDate.isAfter(termLimit)) {
                dates.add(termLimit); // последняя дата - конец срока, для потенциального неполного периода
                break;
            }
            dates.add(nextDate);
            currentDate = nextDate;
        }
        
        return dates;
    }
    
    /**
     * Формула: payment = principal * (dailyRate * (1 + dailyRate)^n) / ((1 + dailyRate)^n - 1)
     */
    private static BigDecimal calculateAnnuityPayment(BigDecimal principal, BigDecimal dailyRate, List<LocalDate> accrualDates) {
        if (accrualDates.isEmpty()) {
            return principal;
        }
        
        int totalPeriods = accrualDates.size();
        BigDecimal rPlusOne = dailyRate.add(BigDecimal.ONE);
        BigDecimal rPlusOnePowN = rPlusOne.pow(totalPeriods);
        
        BigDecimal numerator = dailyRate.multiply(rPlusOnePowN);
        BigDecimal denominator = rPlusOnePowN.subtract(BigDecimal.ONE);
        
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            // Если ставка 0%, просто делим сумму кредита на количество периодов
            return principal.divide(BigDecimal.valueOf(totalPeriods), _calculationScale, RoundingMode.HALF_UP);
        }
        
        return principal.multiply(numerator).divide(denominator, _calculationScale, RoundingMode.HALF_UP);
    }
    
    private static class CalculationParams {
        private final BigDecimal principal;
        private final int months;
        private final BigDecimal dailyRate;
        private final LocalDate startDate;
        private final InterestPeriod interestPeriod;
        
        CalculationParams(BigDecimal principal, int months, BigDecimal dailyRate, LocalDate startDate, InterestPeriod interestPeriod) {
            this.principal = principal;
            this.months = months;
            this.dailyRate = dailyRate;
            this.startDate = startDate;
            this.interestPeriod = interestPeriod;
        }
    }
}