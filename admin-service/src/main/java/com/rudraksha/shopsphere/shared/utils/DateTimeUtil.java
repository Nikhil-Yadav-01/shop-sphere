package com.rudraksha.shopsphere.shared.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public final class DateTimeUtil {

    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";
    public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String DISPLAY_DATE_FORMAT = "dd MMM yyyy";
    public static final String DISPLAY_DATETIME_FORMAT = "dd MMM yyyy HH:mm";

    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern(ISO_DATE_FORMAT);
    public static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ofPattern(ISO_DATETIME_FORMAT);
    public static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern(DISPLAY_DATE_FORMAT);
    public static final DateTimeFormatter DISPLAY_DATETIME = DateTimeFormatter.ofPattern(DISPLAY_DATETIME_FORMAT);

    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    private DateTimeUtil() {
    }

    public static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(ISO_DATE);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(ISO_DATETIME);
    }

    public static String formatInstant(Instant instant) {
        if (instant == null) {
            return null;
        }
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    public static String formatForDisplay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DISPLAY_DATE);
    }

    public static String formatForDisplay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DISPLAY_DATETIME);
    }

    public static Optional<LocalDate> parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(dateStr.trim(), ISO_DATE));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    public static Optional<LocalDateTime> parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDateTime.parse(dateTimeStr.trim(), ISO_DATETIME));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    public static Optional<Instant> parseInstant(String instantStr) {
        if (instantStr == null || instantStr.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(instantStr.trim()));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    public static Instant now() {
        return Instant.now();
    }

    public static LocalDateTime nowLocal() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }

    public static LocalDate today() {
        return LocalDate.now(DEFAULT_ZONE);
    }

    public static Instant toInstant(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(DEFAULT_ZONE).toInstant();
    }

    public static LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, DEFAULT_ZONE);
    }

    public static long daysBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    public static long hoursBetween(Instant start, Instant end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(start, end);
    }

    public static boolean isExpired(Instant expirationTime) {
        if (expirationTime == null) {
            return true;
        }
        return Instant.now().isAfter(expirationTime);
    }

    public static boolean isFuture(Instant instant) {
        if (instant == null) {
            return false;
        }
        return instant.isAfter(Instant.now());
    }

    public static boolean isPast(Instant instant) {
        if (instant == null) {
            return false;
        }
        return instant.isBefore(Instant.now());
    }
}
