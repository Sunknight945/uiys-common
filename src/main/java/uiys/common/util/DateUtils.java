package uiys.common.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateUtils {

    public static final String PATTERN_DATETIME = "yyyy-MM-dd HH:mm:ss";
    public static final String PATTERN_DATE = "yyyy-MM-dd";
    public static final String PATTERN_TIME = "HH:mm:ss";

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern(PATTERN_DATETIME);

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }

    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("无法解析的日期时间: " + dateTimeStr, e);
        }
    }

    // 也可直接使用 Hutool 的 DateUtil，这里提供薄封装

    /**
     * 将纳秒耗时转换为可读的字符串
     * 示例：100ms, 1.23s, 2m 15s, 1h 30m
     */
    public static String formatDuration(long nanos) {
        if (nanos <= 0) {
            return "0ms";
        }
        Duration duration = Duration.ofNanos(nanos);
        long millis = duration.toMillis();

        if (millis < 1000) {
            return millis + "ms";
        }

        long seconds = duration.getSeconds();
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes == 0) {
            return String.format("%.2fs", seconds + (millis % 1000) / 1000.0);
        }

        if (minutes < 60) {
            return String.format("%dm %ds", minutes, remainingSeconds);
        }

        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return String.format("%dh %dm %ds", hours, remainingMinutes, remainingSeconds);
    }
}