package com.artillexstudios.axcalendar.clock.source;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

public interface ClockSource {
    Optional<Long> offsetNanos();

    ZonedDateTime now(ZoneId tz);
}
