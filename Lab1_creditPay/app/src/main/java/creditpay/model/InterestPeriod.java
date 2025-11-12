package creditpay.model;

import java.time.LocalDate;

/**
 * Интерфейс, описывающий период начисления процентов.
 */
public interface InterestPeriod {
    /**
     * Возвращает дату следующего начисления процентов, исходя из даты предыдущего.
     * @param previousAccrualDate дата предыдущего начисления
     * @return следующая дата начисления
     */
    LocalDate nextAccrualDate(LocalDate previousAccrualDate);

    @Override
    String toString();
}
