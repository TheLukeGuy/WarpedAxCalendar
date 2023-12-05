package com.artillexstudios.axcalendar.playtime;

import java.util.UUID;

public final class PlaytimeEntry {
    public final UUID uuid;
    public long nanosPlayedToday;

    public PlaytimeEntry(UUID uuid, long nanosPlayedToday) {
        this.uuid = uuid;
        this.nanosPlayedToday = nanosPlayedToday;
    }

    public String serialize() {
        return this.uuid.toString() + ':' + this.nanosPlayedToday;
    }

    public static PlaytimeEntry deserialize(String lineWithoutLineFeed) {
        int sepIdx = lineWithoutLineFeed.indexOf(':');
        UUID uuid = UUID.fromString(lineWithoutLineFeed.substring(0, sepIdx));
        long nanosPlayedToday = Long.parseLong(lineWithoutLineFeed.substring(sepIdx + 1));
        return new PlaytimeEntry(uuid, nanosPlayedToday);
    }
}
