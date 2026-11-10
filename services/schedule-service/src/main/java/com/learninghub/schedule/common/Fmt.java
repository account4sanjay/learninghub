package com.learninghub.schedule.common;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatting helpers that reproduce the display strings the SPA expects. */
public final class Fmt {
    private Fmt() {}

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
    private static final DateTimeFormatter DATE_SHORT = DateTimeFormatter.ofPattern("MMM d", Locale.US);
    private static final DateTimeFormatter DATE_DOW = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US);
    private static final DateTimeFormatter DATE_DOW_SHORT = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final DateTimeFormatter DATETIME_AT = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a", Locale.US);
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US);
    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US);

    /** "Sep 16, 2026" */
    public static String date(LocalDate d) { return d == null ? null : DATE.format(d); }
    /** "Sep 16" */
    public static String dateShort(LocalDate d) { return d == null ? null : DATE_SHORT.format(d); }
    /** "Mon, Sep 15, 2026" */
    public static String dateWithDow(LocalDate d) { return d == null ? null : DATE_DOW.format(d); }
    /** "Fri, Sep 18" */
    public static String dowShort(LocalDate d) { return d == null ? null : DATE_DOW_SHORT.format(d); }
    /** "September 2026" */
    public static String monthYear(LocalDate d) { return d == null ? null : MONTH_YEAR.format(d); }
    /** "9:00 AM" */
    public static String time(LocalTime t) { return t == null ? null : TIME.format(t); }
    /** "9:00 AM - 10:00 AM" */
    public static String timeRange(LocalTime a, LocalTime b) { return time(a) + " - " + time(b); }
    /** "9:00 - 10:00 AM" or "11:00 AM - 12:00 PM" (drops the first meridiem when both match) */
    public static String timeRangeCompact(LocalTime a, LocalTime b) {
        String s = time(a), e = time(b);
        String ma = s.substring(s.length() - 2), me = e.substring(e.length() - 2);
        return ma.equals(me) ? s.substring(0, s.length() - 3) + " - " + e : s + " - " + e;
    }
    /** "Oct 2, 2026 at 11:59 PM" */
    public static String dateTimeAt(LocalDateTime t) { return t == null ? null : DATETIME_AT.format(t); }
    /** "Oct 2, 2026 11:59 PM" */
    public static String dateTime(LocalDateTime t) { return t == null ? null : DATETIME.format(t); }
    /** "1 hr", "1 hr 30 min", "45 min" */
    public static String duration(LocalTime a, LocalTime b) {
        long m = Duration.between(a, b).toMinutes();
        long h = m / 60, r = m % 60;
        if (h == 0) return r + " min";
        return r == 0 ? h + " hr" : h + " hr " + r + " min";
    }
    /** "7 days 4 hours", "3 hours", "overdue" */
    public static String remaining(LocalDateTime due, LocalDateTime now) {
        Duration d = Duration.between(now, due);
        if (d.isNegative()) return "overdue";
        long days = d.toDays(), hours = d.minusDays(days).toHours();
        if (days == 0) return hours + " hour" + (hours == 1 ? "" : "s");
        return days + " day" + (days == 1 ? "" : "s") + " " + hours + " hour" + (hours == 1 ? "" : "s");
    }
    /** "24:30" from seconds */
    public static String mmss(long seconds) { return String.format("%d:%02d", seconds / 60, seconds % 60); }
    /** "$1,920.00" */
    public static String money(java.math.BigDecimal v) { return java.text.NumberFormat.getCurrencyInstance(Locale.US).format(v); }
}
