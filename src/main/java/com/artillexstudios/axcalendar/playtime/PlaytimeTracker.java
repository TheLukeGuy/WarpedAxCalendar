package com.artillexstudios.axcalendar.playtime;

import com.artillexstudios.axcalendar.clock.WarpedClock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

// This class should only be accessed on the main thread.
public class PlaytimeTracker implements Listener {
    private final Path dataFile;
    private LocalDate day;
    private final List<UUID> onlinePlayers = new ArrayList<>();
    private final Map<UUID, PlaytimeEntry> entries;
    private ZonedDateTime lastUpdate = null;

    public static PlaytimeTracker initWithNoPlayersOnline(Path dataFile) throws IOException {
        ZonedDateTime now = WarpedClock.now();
        LocalDate nowDay = now.toLocalDate();
        Map<UUID, PlaytimeEntry> entries = new HashMap<>();
        if (Files.notExists(dataFile)) {
            return new PlaytimeTracker(dataFile, nowDay, entries);
        }

        List<String> lines = Files.readAllLines(dataFile);
        LocalDate day = LocalDate.parse(lines.get(0));
        if (!nowDay.equals(day)) {
            return new PlaytimeTracker(dataFile, nowDay, entries);
        }
        for (String line : lines.subList(1, lines.size())) {
            PlaytimeEntry entry = PlaytimeEntry.deserialize(line);
            entries.put(entry.uuid, entry);
        }
        return new PlaytimeTracker(dataFile, nowDay, entries);
    }

    private PlaytimeTracker(Path dataFile, LocalDate day, Map<UUID, PlaytimeEntry> entries) {
        this.dataFile = dataFile;
        this.day = day;
        this.entries = entries;
    }

    public long nanosPlayedTodayForUuid(UUID uuid) {
        if (this.entries.containsKey(uuid)) {
            return this.entries.get(uuid).nanosPlayedToday;
        }
        return 0L;
    }

    private PostUpdateAction update() {
        ZonedDateTime now = WarpedClock.now();
        LocalDate nowDay = now.toLocalDate();
        PostUpdateAction postAction;
        if (!nowDay.equals(day)) {
            this.day = nowDay;
            this.entries.clear();
            this.lastUpdate = null;
            postAction = PostUpdateAction.VERY_PROBABLY_SHOULD_SAVE;
        } else {
            postAction = PostUpdateAction.I_DO_NOT_GIVE_A_FUCK;
        }

        long nanosSinceLastUpdate;
        if (this.lastUpdate != null) {
            nanosSinceLastUpdate = this.lastUpdate.until(now, ChronoUnit.NANOS);
        } else {
            nanosSinceLastUpdate = 0L;
        }
        for (UUID uuid : this.onlinePlayers) {
            if (this.entries.containsKey(uuid)) {
                this.entries.get(uuid).nanosPlayedToday += nanosSinceLastUpdate;
            } else {
                this.entries.put(uuid, new PlaytimeEntry(uuid, 0L));
            }
        }

        this.lastUpdate = now;
        return postAction;
    }

    public void saveCurrentState() throws IOException {
        StringBuilder sb = new StringBuilder(11 + (45 * this.entries.size()))
                .append(this.day.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .append('\n');
        for (PlaytimeEntry entry : this.entries.values()) {
            sb.append(entry.serialize()).append('\n');
        }
        Files.writeString(this.dataFile, sb.toString());
    }

    public void updateAndSaveIfRecommended() throws IOException {
        if (this.update() == PostUpdateAction.VERY_PROBABLY_SHOULD_SAVE) {
            this.saveCurrentState();
        }
    }

    public void updateAndSave() throws IOException {
        this.update();
        this.saveCurrentState();
    }

    @EventHandler
    private void handlePlayerJoin(PlayerJoinEvent event) {
        try {
            this.updateAndSaveIfRecommended();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save to the data file.", e);
        }
        UUID uuid = event.getPlayer().getUniqueId();
        this.onlinePlayers.add(uuid);
        if (!this.entries.containsKey(uuid)) {
            this.entries.put(uuid, new PlaytimeEntry(uuid, 0L));
        }
    }

    @EventHandler
    private void handlePlayerQuit(PlayerQuitEvent event) {
        try {
            this.updateAndSaveIfRecommended();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save to the data file.", e);
        }
        this.onlinePlayers.remove(event.getPlayer().getUniqueId());
    }
}
