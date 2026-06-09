package com.huybao.firstmod.command;

import com.huybao.firstmod.data.PlayerChampionData;
import com.huybao.firstmod.network.ModNetworking;
import com.huybao.firstmod.system.ChampionLevelManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * TEMPORARY debug command for testing the champion leveling / stat-screen flow.
 *
 * <ul>
 *   <li>{@code /champion get} – print your current champion data</li>
 *   <li>{@code /champion addxp <n>} – grant XP (triggers level-ups)</li>
 *   <li>{@code /champion open} – force-open the stat screen now</li>
 * </ul>
 *
 * Remove this class (and its {@code register()} call) once testing is done.
 */
public final class ChampionDebugCommand {

    private ChampionDebugCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("champion")
                        .then(CommandManager.literal("get").executes(ChampionDebugCommand::get))
                        .then(CommandManager.literal("open").executes(ChampionDebugCommand::open))
                        .then(CommandManager.literal("addxp")
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ChampionDebugCommand::addXp)))));
    }

    private static int get(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        PlayerChampionData d = player.getAttachedOrCreate(PlayerChampionData.ATTACHMENT);
        ctx.getSource().sendFeedback(() -> Text.literal(String.format(
                "Lv %d | XP %d | points %d | VIT %d STR %d DEX %d DEF %d | needsScreen=%b",
                d.champLevel(), d.champXP(), d.statPoints(),
                d.vitality(), d.strength(), d.dexterity(), d.defence(), d.needsStatScreen())), false);
        return 1;
    }

    private static int addXp(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        ChampionLevelManager.addXP(player, amount);
        PlayerChampionData d = player.getAttachedOrCreate(PlayerChampionData.ATTACHMENT);
        ctx.getSource().sendFeedback(() -> Text.literal(
                "Granted " + amount + " XP. Now Lv " + d.champLevel() + ", points " + d.statPoints()), false);
        return 1;
    }

    private static int open(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        ModNetworking.sendOpenScreen(player, player.getAttachedOrCreate(PlayerChampionData.ATTACHMENT));
        return 1;
    }
}
