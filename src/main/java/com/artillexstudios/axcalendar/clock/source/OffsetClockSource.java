package com.artillexstudios.axcalendar.clock.source;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

public class OffsetClockSource implements ClockSource {
    public final long offsetNanos;

    public OffsetClockSource(long offsetNanos) {
        this.offsetNanos = offsetNanos;
    }

    @Override
    public Optional<Long> offsetNanos() {
        return Optional.of(this.offsetNanos);
    }

    @Override
    public ZonedDateTime now(ZoneId tz) {
        return NormalClockSource.INSTANCE.now(tz).plusNanos(this.offsetNanos);
    }
}
