package com.huybao.firstmod.network;

import com.huybao.firstmod.SwordShieldAndBow;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// C2S: "open my stat sheet" (e.g. the inventory button). No data — the server replies
// with an OpenStatScreenPayload built from the player's real stats.
public record RequestStatScreenPayload() implements CustomPayload {

    public static final CustomPayload.Id<RequestStatScreenPayload> ID =
            new CustomPayload.Id<>(Identifier.of(SwordShieldAndBow.MOD_ID, "request_stat_screen"));

    public static final PacketCodec<RegistryByteBuf, RequestStatScreenPayload> CODEC =
            PacketCodec.unit(new RequestStatScreenPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
