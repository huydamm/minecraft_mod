package com.huybao.firstmod.network;

import com.huybao.firstmod.data.PlayerChampionData;
import com.huybao.firstmod.system.ChampionStatEffects;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

// Custom payloads and their server-side handlers. Call from onInitialize().
public final class ModNetworking {

    private ModNetworking() {
    }

    // Register payload codecs — runs on both sides.
    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(OpenStatScreenPayload.ID, OpenStatScreenPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AllocateStatsPayload.ID, AllocateStatsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestStatScreenPayload.ID, RequestStatScreenPayload.CODEC);
    }

    // Server-side packet handling.
    public static void registerServerSide() {
        // client sent us an allocation
        ServerPlayNetworking.registerGlobalReceiver(AllocateStatsPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            // receivers run off-thread, so hop back to the server thread before touching data
            context.server().execute(() -> applyAllocation(player, payload));
        });

        // client asked us to open the screen (the inventory button)
        ServerPlayNetworking.registerGlobalReceiver(RequestStatScreenPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() ->
                    sendOpenScreen(player, player.getAttachedOrCreate(PlayerChampionData.ATTACHMENT)));
        });
    }

    // Send the open-screen packet built from the given data.
    public static void sendOpenScreen(ServerPlayerEntity player, PlayerChampionData data) {
        ServerPlayNetworking.send(player, new OpenStatScreenPayload(
                data.champLevel(), data.champXP(), data.statPoints(),
                data.vitality(), data.strength(), data.dexterity(), data.defence()));
    }

    private static void applyAllocation(ServerPlayerEntity player, AllocateStatsPayload payload) {
        PlayerChampionData data = player.getAttachedOrCreate(PlayerChampionData.ATTACHMENT);

        // server's authoritative: clamp negatives, reject asking for more than they have
        int addVit = Math.max(0, payload.addVitality());
        int addStr = Math.max(0, payload.addStrength());
        int addDex = Math.max(0, payload.addDexterity());
        int addDef = Math.max(0, payload.addDefence());
        int requested = addVit + addStr + addDex + addDef;

        if (requested > data.statPoints()) {
            return; // bogus request, ignore it
        }

        PlayerChampionData updated = data
                .withStatPoints(data.statPoints() - requested)
                .withVitality(data.vitality() + addVit)
                .withStrength(data.strength() + addStr)
                .withDexterity(data.dexterity() + addDex)
                .withDefence(data.defence() + addDef)
                .withNeedsStatScreen(false);

        player.setAttached(PlayerChampionData.ATTACHMENT, updated);

        // refresh health / effects for the new totals
        ChampionStatEffects.apply(player);
    }
}
