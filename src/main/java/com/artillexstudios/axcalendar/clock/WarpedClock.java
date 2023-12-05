package com.artillexstudios.axcalendar.clock;

import com.artillexstudios.axcalendar.AxCalendar;
import com.artillexstudios.axcalendar.clock.source.ClockSource;
import com.artillexstudios.axcalendar.clock.source.NormalClockSource;
import com.artillexstudios.axcalendar.clock.source.OffsetClockSource;

import java.time.Duration;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

// This is so warped! Holy shit! This clock is not monotonic, and it's probably pretty close to wall time. But it could,
// at the request of the server administrator, report the current year as 1985. Or 2097. Or it could be 7 PM when really
// it's 7 AM. With that in mind, this clock is the base for ALL timekeeping in the plugin. After all, if you're going to
// run a _warped_ realm, it's only fitting to have a _warped_ clock. This comment is not cringe! It's informative. Oh,
// I'm the only one reading it. Am I procrastinating? Do I actually have no idea how I'm going to implement this? Psh.
//
// This class should only be accessed on the main thread.
public final class WarpedClock {
    private static ZoneId tz = null;
    private static Month activeMonth = null;
    private static ClockSource source = NormalClockSource.INSTANCE;

    private WarpedClock() {}

    public static void reload() {
        tz = null;
        activeMonth = null;
    }

    public static Optional<Long> offsetNanos() {
        return source.offsetNanos();
    }

    public static void setOffset(long offsetNanos) {
        if (offsetNanos == 0) {
            source = NormalClockSource.INSTANCE;
        } else {
            source = new OffsetClockSource(offsetNanos);
        }
    }

    public static ZonedDateTime now() {
        return source.now(tz());
    }

    public static int dayOfMonth() {
        return now().getDayOfMonth();
    }

    public static boolean isInActiveMonth() {
        return now().getMonthValue() == activeMonth().getValue();
    }

    public static Duration durationUntilDayOfMonth(int dayOfMonth) {
        ZonedDateTime now = now();
        ZonedDateTime then = now.withDayOfMonth(dayOfMonth).withHour(0).withMinute(0).withSecond(0).withNano(0);
        return Duration.between(now, then);
    }

    public static String dbgFmt() {
        return now().format(DateTimeFormatter.ofPattern("E MMM d HH:mm:ss z uuuu"));
    }

    private static ZoneId tz() {
        if (tz == null) {
            String tzStr = AxCalendar.config.getString("timezone", "");
            if (!tzStr.isBlank()) {
                tz = ZoneId.of(tzStr);
            } else {
                tz = ZoneId.systemDefault();
            }
        }
        return tz;
    }

    private static Month activeMonth() {
        if (activeMonth == null) {
            String activeMonthStr = AxCalendar.config.getString("month", null);
            if (activeMonthStr != null) {
                activeMonth = Month.valueOf(activeMonthStr);
            } else {
                activeMonth = Month.DECEMBER;
            }
        }
        return activeMonth;
    }
}
