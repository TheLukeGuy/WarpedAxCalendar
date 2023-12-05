package com.artillexstudios.axcalendar.cmd;

import com.artillexstudios.axcalendar.AxCalendar;
import com.artillexstudios.axcalendar.clock.WarpedClock;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;
import java.util.Collection;

public final class Cmds {
    private static final LiteralArgumentBuilder<CommandSourceStack> openBuilder = Commands.literal("open")
            .requires(CommandSourceStack::isPlayer)
            .executes(OpenCmdExclusionZone::execOpen);

    private static final LiteralArgumentBuilder<CommandSourceStack> reloadBuilder = Commands.literal("reload")
            .requires(Cmds::hasAdmin)
            .executes(Cmds::execReload);

    private static final LiteralArgumentBuilder<CommandSourceStack> resetBuilder = Commands.literal("reset")
            .requires(Cmds::hasAdmin)
            .then(Commands.argument("players", EntityArgument.players()).executes(Cmds::execReset));

    private static final LiteralArgumentBuilder<CommandSourceStack> clockBuilder = Commands.literal("clock")
            .requires(Cmds::hasAdmin)
            .then(Commands.literal("status").executes(ctx -> execClock(ctx, null)))
            .then(
                    Commands.literal("offset")
                            .then(
                                    Commands.literal("set").then(
                                            Commands.argument("nanos", LongArgumentType.longArg())
                                                    .executes(ctx -> execClock(
                                                            ctx,
                                                            LongArgumentType.getLong(ctx, "nanos")
                                                    ))
                                    )
                            )
                            .then(
                                    Commands.literal("add").then(
                                            Commands.argument("nanos", LongArgumentType.longArg())
                                                    .executes(ctx -> {
                                                        long current = WarpedClock.offsetNanos().orElse(0L);
                                                        long addition = LongArgumentType.getLong(ctx, "nanos");
                                                        return execClock(ctx, current + addition);
                                                    })
                                    )
                            )
            );

    public static final LiteralCommandNode<CommandSourceStack> NODE = Commands.literal(AxCalendar.CMD_NAME)
            .then(openBuilder)
            .then(reloadBuilder)
            .then(resetBuilder)
            .then(clockBuilder)
            .build();

    private Cmds() {}

    private static int execReload(CommandContext<CommandSourceStack> ctx) {
        if (!AxCalendar.config.reload()) {
            sendMsg(ctx, Component.text("Failed to reload the configuration file.", NamedTextColor.RED));
            return 0;
        }
        if (!AxCalendar.shitOldMsgsForUseOnlyInTheExclusionZone.reload()) {
            sendMsg(ctx, Component.text("Failed to reload the message file.", NamedTextColor.RED));
            return 0;
        }

        AxCalendar.reload();
        WarpedClock.reload();

        AxCalendar.getDatabase().disable();
        AxCalendar.getDatabase().setup();

        sendMsg(
                ctx,
                Component.text("Successfully reloaded {config,messages}.yml and the database!", NamedTextColor.GREEN)
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int execReset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        players
                .stream()
                .map(ServerPlayer::getBukkitEntity)
                .forEach(player -> AxCalendar.getDatabase().reset(player.getUniqueId()));
        sendMsg(ctx, Component.text("Successfully reset data for the player(s).", NamedTextColor.GREEN));
        return players.size();
    }

    private static int execClock(CommandContext<CommandSourceStack> ctx, Long newOffset) {
        if (newOffset != null) {
            WarpedClock.setOffset(newOffset);
        }
        sendMsg(ctx, Component.text("The clock reports " + WarpedClock.dbgFmt() + '.'));
        Component offsetStatus = WarpedClock.offsetNanos()
                .map(offset -> {
                    String preposition;
                    if (offset < 0) {
                        preposition = "behind";
                    } else {
                        preposition = "ahead of";
                    }
                    String fmtOffset = AxCalendar.fmtDuration(Duration.ofNanos(Math.abs(offset)), true);
                    return Component.text(
                            "The clock is " + fmtOffset + ' ' + preposition + " system time.",
                            NamedTextColor.YELLOW
                    );
                })
                .orElse(Component.text("The clock is in sync with system time.", NamedTextColor.GREEN));
        sendMsg(ctx, offsetStatus);
        return Command.SINGLE_SUCCESS;
    }

    private static boolean hasAdmin(CommandSourceStack src) {
        return src.getBukkitSender().hasPermission("axcalendar.admin");
    }

    private static void sendMsg(CommandContext<CommandSourceStack> ctx, ComponentLike msg) {
        AxCalendar.sendMsg(ctx.getSource().getBukkitSender(), msg);
    }
}
