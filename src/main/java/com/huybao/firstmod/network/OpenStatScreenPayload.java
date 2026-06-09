package com.huybao.firstmod.network;

import com.huybao.firstmod.SwordShieldAndBow;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C: tells the client to open the stat sheet, carrying everything it needs to display.
 */
public record OpenStatScreenPayload(
        int championLevel,
        int champXP,
        int statPoints,
        int vitality,
        int strength,
        int dexterity,
        int defence
) implements CustomPayload {

    public static final CustomPayload.Id<OpenStatScreenPayload> ID =
            new CustomPayload.Id<>(Identifier.of(SwordShieldAndBow.MOD_ID, "open_stat_screen"));

    public static final PacketCodec<RegistryByteBuf, OpenStatScreenPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, OpenStatScreenPayload::championLevel,
            PacketCodecs.VAR_INT, OpenStatScreenPayload::champXP,
            PacketCodecs.VAR_INT, OpenStatScreenPayload::statPoints,
            PacketCodecs.VAR_INT, OpenStatScreenPayload::vitality,
            PacketCodecs.VAR_INT, OpenStatScreenPayload::strength,
            PacketCodecs.VAR_INT, OpenStatScreenPayload::dexterity,
            PacketCodecs.VAR_INT, OpenStatScreenPayload::defence,
            OpenStatScreenPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
