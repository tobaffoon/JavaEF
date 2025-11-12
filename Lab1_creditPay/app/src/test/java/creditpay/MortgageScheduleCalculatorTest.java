package creditpay;

import org.junit.jupiter.api.Test;

import creditpay.MortgageScheduleCalculator;
import creditpay.model.CreditTerms;

import java.util.List;

public class MortgageScheduleCalculatorTest {
    @Test
    public void testDifferentiated() {
        CreditTerms terms = CreditTerms.sample();
        List<MortgageScheduleCalculator.Payment> schedule = MortgageScheduleCalculator.differentiated(terms);
        System.out.println("Differentiated schedule:");
        for (int i = 0; i < Math.min(3, schedule.size()); i++) {
            System.out.println(schedule.get(i));
        }
    }

    @Test
    public void testAnnuity() {
        CreditTerms terms = CreditTerms.sample();
        List<MortgageScheduleCalculator.Payment> schedule = MortgageScheduleCalculator.annuity(terms);
        System.out.println("Annuity schedule:");
        for (int i = 0; i < Math.min(3, schedule.size()); i++) {
            System.out.println(schedule.get(i));
        }
    }
}
