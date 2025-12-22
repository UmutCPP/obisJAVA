package util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Tarih formatlama/parsing için küçük yardımcı sınıf.
public final class TarihUtil {
    private TarihUtil() {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static String format(LocalDate date) {
        return date == null ? "" : date.format(DATE_FMT);
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATETIME_FMT);
    }

    public static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s.trim(), DATE_FMT);
    }

    public static LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDateTime.parse(s.trim(), DATETIME_FMT);
    }
}
