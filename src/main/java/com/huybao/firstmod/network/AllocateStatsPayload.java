package com.huybao.firstmod.network;

import com.huybao.firstmod.SwordShieldAndBow;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S: the player's chosen allocation — how many points to add to each stat.
 * The server validates this against the player's available statPoints.
 */
public record AllocateStatsPayload(
        int addVitality,
        int addStrength,
        int addDexterity,
        int addDefence
) implements CustomPayload {

    public static final CustomPayload.Id<AllocateStatsPayload> ID =
            new CustomPayload.Id<>(Identifier.of(SwordShieldAndBow.MOD_ID, "allocate_stats"));

    public static final PacketCodec<RegistryByteBuf, AllocateStatsPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, AllocateStatsPayload::addVitality,
            PacketCodecs.VAR_INT, AllocateStatsPayload::addStrength,
            PacketCodecs.VAR_INT, AllocateStatsPayload::addDexterity,
            PacketCodecs.VAR_INT, AllocateStatsPayload::addDefence,
            AllocateStatsPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
