package com.artillexstudios.axcalendar.shitoldutils;

import com.artillexstudios.axcalendar.AxCalendar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ActionUtils {
    public static void handleAction(@NotNull HumanEntity player, @NotNull String str, int day) {
        if (str.startsWith("[CLOSE]")) {
            player.closeInventory();
        } else if (str.startsWith("[CONSOLE]")) {
            str = str.replace("[CONSOLE] ", "");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), str.replace("%day%", "" + day).replace("%player%", player.getName()));
        } else if (str.startsWith("[MESSAGE]")) {
            str = str.replace("[MESSAGE] ", "");
            str = str.replace("%day%", "" + day).replace("%player%", player.getName());
            AxCalendar.sendMsg(player, MiniMessage.miniMessage().deserialize(str));
        } else if (str.startsWith("[PLAYER]")) {
            str = str.replace("[PLAYER] ", "");
            Bukkit.dispatchCommand(player, str.replace("%day%", "" + day).replace("%player%", player.getName()));
        } else if (str.startsWith("[SOUND]")) {
            str = str.replace("[SOUND] ", "");
            ((Player) player).playSound(player.getLocation(), Sound.valueOf(str), 1, 1);
        }
    }
}
