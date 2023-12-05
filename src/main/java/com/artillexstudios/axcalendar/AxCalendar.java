package com.artillexstudios.axcalendar;

import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.dvs.versioning.BasicVersioning;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.route.Route;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.dumper.DumperSettings;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.general.GeneralSettings;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.loader.LoaderSettings;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.updater.UpdaterSettings;
import com.artillexstudios.axapi.nms.NMSHandlers;
import com.artillexstudios.axapi.utils.Version;
import com.artillexstudios.axcalendar.cmd.Cmds;
import com.artillexstudios.axcalendar.database.Database;
import com.artillexstudios.axcalendar.database.H2;
import com.artillexstudios.axcalendar.playtime.PlaytimeTracker;
import com.destroystokyo.paper.event.brigadier.CommandRegisteredEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.craftbukkit.v1_20_R1.CraftServer;
import org.bukkit.craftbukkit.v1_20_R1.command.VanillaCommandWrapper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

public final class AxCalendar extends JavaPlugin implements Listener {
    public static final String CMD_NAME = "calendar";

    public static Config config;
    public static Config shitOldMsgsForUseOnlyInTheExclusionZone;

    private static Database database;
    private static PlaytimeTracker playTimeTracker = null;

    private static Long requiredDayPlaytimeNanosToClaim = null;
    private static Component prefix = null;

    public static void reload() {
        requiredDayPlaytimeNanosToClaim = null;
        prefix = null;
    }

    private static long requiredDayPlaytimeNanosToClaim() {
        if (requiredDayPlaytimeNanosToClaim == null) {
            requiredDayPlaytimeNanosToClaim = config.getLong("required-day-playtime-to-claim", 0L);
        }
        return requiredDayPlaytimeNanosToClaim;
    }

    public static long playtimeRemainingNanos(OfflinePlayer player) {
        long required = requiredDayPlaytimeNanosToClaim();
        if (required == 0L) {
            return 0L;
        }

        try {
            playTimeTracker.updateAndSaveIfRecommended();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save the playtime tracker.", e);
        }
        return required - playTimeTracker.nanosPlayedTodayForUuid(player.getUniqueId());
    }

    public static Database getDatabase() {
        return database;
    }

    private static Component prefix() {
        if (prefix == null) {
            String prefixStr = config.getString("prefix", "");
            prefix = MiniMessage.miniMessage().deserialize(prefixStr);
        }
        return prefix;
    }

    public static void sendMsg(CommandSender sender, ComponentLike msg) {
        Component full = prefix().append(msg);
        sender.sendMessage(full);
    }

    private static String fmtDurationPart(String unit, long value) {
        String unitWord = (value == 1) ? unit : (unit + 's');
        return value + " " + unitWord;
    }

    public static String fmtDuration(Duration duration, boolean compact) {
        long[] parts = {
                duration.toDaysPart(), duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart(),
        };
        if (compact) {
            StringBuilder sb = new StringBuilder(11);
            for (int i = 0; i < parts.length; i++) {
                sb.append(String.format("%02d", parts[i]));
                if (i != parts.length - 1) {
                    sb.append(':');
                }
            }
            return sb.toString();
        }

        if (parts[0] != 0) {
            return fmtDurationPart("day", parts[0]);
        }
        if (parts[1] != 0) {
            return fmtDurationPart("hour", parts[1]);
        }
        if (parts[2] != 0) {
            return fmtDurationPart("minute", parts[2]);
        }
        return fmtDurationPart("second", parts[3]);
    }

    @Override
    public void onEnable() {
        Version ignoredVersion = Version.UNKNOWN;
        NMSHandlers.initialise(this); // brit alert 🚨
        LoggerFactory.getLogger("Luke").info("^ shut the fuck up literally nobody asked for the server version");

        config = new Config(new File(getDataFolder(), "config.yml"), getResource("config.yml"), GeneralSettings.builder().setUseDefaults(false).build(), LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().setVersioning(new BasicVersioning("version")).build());
        shitOldMsgsForUseOnlyInTheExclusionZone = new Config(new File(getDataFolder(), "messages.yml"), getResource("messages.yml"), GeneralSettings.builder().setUseDefaults(false).build(), LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().addIgnoredRoute("2", Route.from("menu")).setVersioning(new BasicVersioning("version")).build());

        try {
            if (playTimeTracker == null) {
                Path playTimeTrackerDataFile = this.getDataFolder().toPath().resolve("playtime.txt");
                playTimeTracker = PlaytimeTracker.initWithNoPlayersOnline(playTimeTrackerDataFile);
            }
            playTimeTracker.updateAndSaveIfRecommended();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to initialize the playtime tracker.", e);
        }
        Bukkit.getPluginManager().registerEvents(playTimeTracker, this);

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getCommandMap().register(this.getName(), new BukkitCmd());

        database = new H2(this.getDataFolder().toPath());
        database.setup();
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.disable();
        }
        if (playTimeTracker != null) {
            try {
                playTimeTracker.updateAndSave();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to save the playtime tracker.", e);
            }
        }
    }

    @EventHandler
    private void handleCmdRegister(CommandRegisteredEvent<CommandSourceStack> event) {
        if (event.getCommand() instanceof BukkitCmd) {
            event.setLiteral(Cmds.NODE);
            Commands dispatcher = ((CraftServer) this.getServer()).getServer().resources.managers().commands;
            dispatcher.vanillaCommandNodes.add(Cmds.NODE);
        }
    }

    private class BukkitCmd extends Command implements PluginIdentifiableCommand {
        public BukkitCmd() {
            super(CMD_NAME, "Use or manage the Advent calendar.", "/" + CMD_NAME + " [arg ...]", List.of());
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
            StringBuilder cmdBuilder = new StringBuilder(label);
            if (args.length != 0) {
                cmdBuilder.append(' ').append(String.join(" ", args));
            }
            ((CraftServer) AxCalendar.this.getServer()).getServer().getCommands().performPrefixedCommand(
                    VanillaCommandWrapper.getListener(sender),
                    cmdBuilder.toString(),
                    label
            );
            return true;
        }

        @Override
        public @NotNull List<String> tabComplete(
                @NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args
        ) {
            return List.of();
        }

        @Override
        public @NotNull Plugin getPlugin() {
            return AxCalendar.this;
        }
    }
}
