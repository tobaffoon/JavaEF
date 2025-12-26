package stockmarket.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class TimeUtils {
    public static final String TIME_FORMAT = "HH:mm:ss";
    public static final String DATE_FORMAT = "dd.MM.yyyy";

    public static boolean isValidDate(String input) {
        try {
            LocalDateTime.parse(input, DateTimeFormatter.ofPattern(DATE_FORMAT));
        } catch (DateTimeParseException e) {
            return false;
        }

        return true;
    }

    public static boolean isValidTime(String input) {

        try {
            LocalDateTime.parse(input, DateTimeFormatter.ofPattern(TIME_FORMAT));
        } catch (DateTimeParseException e) {
            return false;
        }

        return true;
    }

    public static LocalDateTime parseToLocalDateTime(String date, String time) {
        return LocalDateTime.of(
                LocalDate.parse(date, DateTimeFormatter.ofPattern(DATE_FORMAT)),
                LocalTime.parse(time, DateTimeFormatter.ofPattern(TIME_FORMAT))
        );
    }
}

