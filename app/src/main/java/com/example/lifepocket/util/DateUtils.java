package com.example.lifepocket.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Central place for local-day boundaries and friendly due labels. */
public final class DateUtils {
    private DateUtils() {}

    public static long startOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long endOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startOfDay(millis));
        c.add(Calendar.DAY_OF_YEAR, 1);
        return c.getTimeInMillis() - 1;
    }

    /** Effective moment an item becomes overdue (date-only items are due at end of day). */
    public static long effectiveDue(long dueAt, boolean hasTime) {
        return hasTime ? dueAt : endOfDay(dueAt);
    }

    public static String time(long millis) {
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(millis));
    }

    public static String fullDate(long millis) {
        return new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date(millis));
    }

    public static String shortDate(long millis) {
        return new SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(new Date(millis));
    }

    public static String relativeDay(long millis) {
        long today = startOfDay(System.currentTimeMillis());
        long day = startOfDay(millis);
        long diff = Math.round((day - today) / 86_400_000.0);
        if (diff == 0) return "Today";
        if (diff == 1) return "Tomorrow";
        if (diff == -1) return "Yesterday";
        return shortDate(millis);
    }

    public static String dueLabel(Long dueAt, boolean hasTime) {
        if (dueAt == null) return "No due date";
        String day = relativeDay(dueAt);
        return hasTime ? day + ", " + time(dueAt) : day;
    }

    public static String greeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h < 12) return "Good morning";
        if (h < 17) return "Good afternoon";
        return "Good evening";
    }
}
