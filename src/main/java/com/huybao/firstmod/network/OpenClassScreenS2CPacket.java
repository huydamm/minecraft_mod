package com.huybao.firstmod.network;

import com.huybao.firstmod.SwordShieldAndBow;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// S2C: pure signal — "open the class selection screen". No payload needed.
public record OpenClassScreenS2CPacket() implements CustomPayload {

    public static final CustomPayload.Id<OpenClassScreenS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(SwordShieldAndBow.MOD_ID, "open_class_screen"));

    public static final PacketCodec<RegistryByteBuf, OpenClassScreenS2CPacket> CODEC =
            PacketCodec.unit(new OpenClassScreenS2CPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
