package com.artillexstudios.axcalendar.clock.source;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

public class NormalClockSource implements ClockSource {
    public static final NormalClockSource INSTANCE = new NormalClockSource();

    @Override
    public Optional<Long> offsetNanos() {
        return Optional.empty();
    }

    @Override
    public ZonedDateTime now(ZoneId tz) {
        return ZonedDateTime.now(tz);
    }
}
