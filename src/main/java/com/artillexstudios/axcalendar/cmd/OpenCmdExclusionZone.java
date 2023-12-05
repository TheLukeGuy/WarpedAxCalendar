package com.artillexstudios.axcalendar.cmd;

import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.utils.ItemBuilder;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axcalendar.AxCalendar;
import com.artillexstudios.axcalendar.clock.WarpedClock;
import com.artillexstudios.axcalendar.shitoldutils.ActionUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.commands.CommandSourceStack;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;

import static com.artillexstudios.axcalendar.AxCalendar.config;
import static com.artillexstudios.axcalendar.AxCalendar.shitOldMsgsForUseOnlyInTheExclusionZone;

public final class OpenCmdExclusionZone {
    private OpenCmdExclusionZone() {}

    public static int execOpen(CommandContext<CommandSourceStack> ctx) {
        Player player = (Player) ctx.getSource().getBukkitSender();

        // Okay, go wild now...

        final Gui menu = Gui.gui()
                .title(StringUtils.format(shitOldMsgsForUseOnlyInTheExclusionZone.getString("menu.title")))
                .rows(shitOldMsgsForUseOnlyInTheExclusionZone.getInt("menu.rows", 6))
                .disableAllInteractions()
                .create();

        if (shitOldMsgsForUseOnlyInTheExclusionZone.getSection("menu.filler") != null)
            menu.getFiller().fill(new GuiItem(new ItemBuilder(shitOldMsgsForUseOnlyInTheExclusionZone.getSection("menu.filler")).get()));

        if (shitOldMsgsForUseOnlyInTheExclusionZone.getSection("menu.other") != null) {
            for (String str : shitOldMsgsForUseOnlyInTheExclusionZone.getSection("menu.other").getRoutesAsStrings(false)) {
                final GuiItem guiItem = new GuiItem(new ItemBuilder(shitOldMsgsForUseOnlyInTheExclusionZone.getSection("menu.other." + str + ".item")).get());
                guiItem.setAction(event -> {
                    for (String str2 : shitOldMsgsForUseOnlyInTheExclusionZone.getStringList("menu.other." + str + ".actions")) {
                        ActionUtils.handleAction(event.getWhoClicked(), str2, -1);
                    }
                });
                menu.setItem(shitOldMsgsForUseOnlyInTheExclusionZone.getInt("menu.other." + str + ".slot"), guiItem);
            }
        }

        for (String str : shitOldMsgsForUseOnlyInTheExclusionZone.getSection("menu.days").getRoutesAsStrings(false)) {
            int day = Integer.parseInt(str);
            boolean claimed = AxCalendar.getDatabase().isClaimed(player, day);
            int dayOfMonth = WarpedClock.dayOfMonth();
            String type;

            final HashMap<String, String> replacements = new HashMap<>();
            replacements.put("%day%", "" + day);
            replacements.put("%time%", AxCalendar.fmtDuration(WarpedClock.durationUntilDayOfMonth(day), true));

            if (claimed) type = "claimed";
            else if (!WarpedClock.isInActiveMonth() || dayOfMonth < day) type = "unclaimable";
            else type = "claimable";

            final Section section = shitOldMsgsForUseOnlyInTheExclusionZone.getSection("menu.days." + str + ".item-" + type);
            final GuiItem guiItem = new GuiItem(new ItemBuilder(section).setName(section.getString("name"), replacements).setLore(section.getStringList("lore"), replacements).get());
            menu.setItem(shitOldMsgsForUseOnlyInTheExclusionZone.getInt("menu.days." + str + ".slot"), guiItem);

            guiItem.setAction(event -> {
                int newDayOfMonth = WarpedClock.dayOfMonth();

                if (WarpedClock.isInActiveMonth() && newDayOfMonth > day && !config.getBoolean("allow-late-claiming", true)) {
                    AxCalendar.sendMsg(
                            player, Component.text("You can't claim that present anymore!", NamedTextColor.RED)
                    );
                    return;
                }

                if (!WarpedClock.isInActiveMonth() || newDayOfMonth < day) {
                    String duration = AxCalendar.fmtDuration(WarpedClock.durationUntilDayOfMonth(day), false);
                    AxCalendar.sendMsg(
                            player,
                            Component.text(
                                    "It's too early to claim that present! Come back in " + duration + ".",
                                    NamedTextColor.RED
                            )
                    );
                    return;
                }

                if (AxCalendar.getDatabase().isClaimed(player, day)) {
                    AxCalendar.sendMsg(
                            player, Component.text("You already claimed that present!", NamedTextColor.RED)
                    );
                    return;
                }

                if (config.getInt("max-accounts-per-ip", 3) <= AxCalendar.getDatabase().countIps(player, day)) {
                    AxCalendar.sendMsg(
                            player, Component.text("Too many accounts on your network already claimed that present!")
                    );
                    return;
                }

                long playtimeRemaining = AxCalendar.playtimeRemainingNanos(player);
                if (playtimeRemaining > 0L) {
                    String duration = AxCalendar.fmtDuration(Duration.ofNanos(playtimeRemaining), false);
                    AxCalendar.sendMsg(
                            player,
                            Component.text(
                                    "You haven't played for long enough to open that present! Come back in "
                                            + duration
                                            + ".",
                                    NamedTextColor.RED
                            )
                    );
                    return;
                }

                AxCalendar.getDatabase().claim(player, day);

                for (String str2 : shitOldMsgsForUseOnlyInTheExclusionZone.getStringList("menu.days." + str + ".actions")) {
                    ActionUtils.handleAction(event.getWhoClicked(), str2, day);
                }
                execOpen(ctx);
            });
        }

        menu.open(player);

        // That was wild

        return Command.SINGLE_SUCCESS;
    }
}
