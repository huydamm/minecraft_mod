package com.huybao.firstmod.network;

import com.huybao.firstmod.data.PlayerChampionData;
import com.huybao.firstmod.system.ChampionStatEffects;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Registers the mod's custom payloads, the server-side receivers, and the join hook that
 * re-opens the stat screen for players who still owe an allocation. Call from onInitialize().
 */
public final class ModNetworking {

    private ModNetworking() {
    }

    /** Registers payload codecs. Must run on both client and server (common init). */
    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(OpenStatScreenPayload.ID, OpenStatScreenPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AllocateStatsPayload.ID, AllocateStatsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestStatScreenPayload.ID, RequestStatScreenPayload.CODEC);
    }

    /** Registers server-side packet handling and the join resend. */
    public static void registerServerSide() {
        // Apply an allocation the client sent us.
        ServerPlayNetworking.registerGlobalReceiver(AllocateStatsPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            // Networking callbacks run off-thread; hop back onto the server thread to touch data.
            context.server().execute(() -> applyAllocation(player, payload));
        });

        // Client (e.g. the inventory button) asked us to open the screen.
        ServerPlayNetworking.registerGlobalReceiver(RequestStatScreenPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() ->
                    sendOpenScreen(player, player.getAttachedOrCreate(PlayerChampionData.ATTACHMENT)));
        });
    }

    /** Sends the S2C open-screen packet built from the given data. */
    public static void sendOpenScreen(ServerPlayerEntity player, PlayerChampionData data) {
        ServerPlayNetworking.send(player, new OpenStatScreenPayload(
                data.champLevel(), data.champXP(), data.statPoints(),
                data.vitality(), data.strength(), data.dexterity(), data.defence()));
    }

    private static void applyAllocation(ServerPlayerEntity player, AllocateStatsPayload payload) {
        PlayerChampionData data = player.getAttachedOrCreate(PlayerChampionData.ATTACHMENT);

        // Clamp negatives and reject over-allocation (the server is authoritative).
        int addVit = Math.max(0, payload.addVitality());
        int addStr = Math.max(0, payload.addStrength());
        int addDex = Math.max(0, payload.addDexterity());
        int addDef = Math.max(0, payload.addDefence());
        int requested = addVit + addStr + addDex + addDef;

        if (requested > data.statPoints()) {
            return; // invalid client request; ignore
        }

        PlayerChampionData updated = data
                .withStatPoints(data.statPoints() - requested)
                .withVitality(data.vitality() + addVit)
                .withStrength(data.strength() + addStr)
                .withDexterity(data.dexterity() + addDex)
                .withDefence(data.defence() + addDef)
                .withNeedsStatScreen(false); // acknowledged

        player.setAttached(PlayerChampionData.ATTACHMENT, updated);

        // Re-derive max health / status effects from the new stat totals.
        ChampionStatEffects.apply(player);
    }
}
