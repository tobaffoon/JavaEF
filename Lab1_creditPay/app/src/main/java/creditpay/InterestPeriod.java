package creditpay;

import java.time.LocalDate;

/**
 * Интерфейс, описывающий период начисления процентов.
 * Метод nextAccrualDate возвращает дату следующего начисления процентов
 * по дате предыдущего начисления.
 */
public interface InterestPeriod {
    /**
     * Возвращает дату следующего начисления процентов, исходя из даты предыдущего.
     * @param previousAccrualDate дата предыдущего начисления (не null)
     * @return следующая дата начисления
     */
    LocalDate nextAccrualDate(LocalDate previousAccrualDate);

    @Override
    String toString();
}
