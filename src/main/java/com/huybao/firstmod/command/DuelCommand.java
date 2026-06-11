package com.huybao.firstmod.command;

import com.huybao.firstmod.system.DuelManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

// /duel accept | /duel deny — the request itself comes from right-clicking a player.
public final class DuelCommand {

    private DuelCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("duel")
                        .then(CommandManager.literal("accept").executes(DuelCommand::accept))
                        .then(CommandManager.literal("deny").executes(DuelCommand::deny))));
    }

    private static int accept(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        DuelManager.accept(ctx.getSource().getPlayerOrThrow());
        return 1;
    }

    private static int deny(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        DuelManager.deny(ctx.getSource().getPlayerOrThrow());
        return 1;
    }
}
