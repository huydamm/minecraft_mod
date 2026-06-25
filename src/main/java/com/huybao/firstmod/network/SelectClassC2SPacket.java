package com.huybao.firstmod.network;

import com.huybao.firstmod.SwordShieldAndBow;
import com.huybao.firstmod.data.PlayerClass;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// C2S: the class the player clicked, sent as the enum ordinal. Server re-validates.
public record SelectClassC2SPacket(int classId) implements CustomPayload {

    public static final CustomPayload.Id<SelectClassC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(SwordShieldAndBow.MOD_ID, "select_class"));

    public static final PacketCodec<RegistryByteBuf, SelectClassC2SPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, SelectClassC2SPacket::classId,
            SelectClassC2SPacket::new
    );

    public SelectClassC2SPacket(PlayerClass playerClass) {
        this(playerClass.ordinal());
    }

    public PlayerClass playerClass() {
        return PlayerClass.byId(classId);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
