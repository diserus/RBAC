import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public class DateUtils {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String getCurrentDate() {
        return LocalDate.now().format(DATE_FMT);
    }

    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DATETIME_FMT);
    }

    public static boolean isBefore(String date1, String date2) {
        if (date1 == null || date2 == null) return false;
        return date1.compareTo(date2) < 0;
    }

    public static boolean isAfter(String date1, String date2) {
        if (date1 == null || date2 == null) return false;
        return date1.compareTo(date2) > 0;
    }

    public static String addDays(String date, int days) {
        if (date == null) throw new IllegalArgumentException("date не может быть null");
        try {
            LocalDate ld = LocalDate.parse(date, DATE_FMT);
            return ld.plusDays(days).format(DATE_FMT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Неверный формат даты: " + date + " (ожидается YYYY-MM-DD)");
        }
    }

    public static String formatRelativeTime(String date) {
        if (date == null) return "unknown";
        try {
            LocalDate target = LocalDate.parse(date, DATE_FMT);
            LocalDate today  = LocalDate.now();
            long diff = ChronoUnit.DAYS.between(today, target); // положительный = будущее

            if (diff == 0)        return "today";
            if (diff == -1)       return "yesterday";
            if (diff == 1)        return "tomorrow";
            if (diff < 0)         return Math.abs(diff) + " days ago";
            return "in " + diff + " days";
        } catch (DateTimeParseException e) {
            return date;
        }
    }
}