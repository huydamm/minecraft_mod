package com.huybao.firstmod.network;

import com.huybao.firstmod.SwordShieldAndBow;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// C2S: "I right-clicked this player and picked Duel." Carries the target's entity id;
// the server resolves it and validates before sending the request.
public record ChallengePlayerPayload(int targetEntityId) implements CustomPayload {

    public static final CustomPayload.Id<ChallengePlayerPayload> ID =
            new CustomPayload.Id<>(Identifier.of(SwordShieldAndBow.MOD_ID, "challenge_player"));

    public static final PacketCodec<RegistryByteBuf, ChallengePlayerPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, ChallengePlayerPayload::targetEntityId,
            ChallengePlayerPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
