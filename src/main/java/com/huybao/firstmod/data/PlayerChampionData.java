package com.huybao.firstmod.data;

import com.huybao.firstmod.SwordShieldAndBow;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;

// Per-player champion data: level, XP, stat points, the 4 stats, and the "owed a screen" flag.
public record PlayerChampionData(
        int champLevel,
        int champXP,
        int statPoints,
        int vitality,
        int strength,
        int defence,
        int dexterity,
        boolean needsStatScreen
) {
    public static final PlayerChampionData DEFAULT = new PlayerChampionData(0, 0, 0, 0, 0, 0, 0, false);

    // Save codec. optionalFieldOf means old saves / new fields just default instead of crashing.
    public static final Codec<PlayerChampionData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("champLevel", 0).forGetter(PlayerChampionData::champLevel),
            Codec.INT.optionalFieldOf("champXP", 0).forGetter(PlayerChampionData::champXP),
            Codec.INT.optionalFieldOf("statPoints", 0).forGetter(PlayerChampionData::statPoints),
            Codec.INT.optionalFieldOf("vitality", 0).forGetter(PlayerChampionData::vitality),
            Codec.INT.optionalFieldOf("strength", 0).forGetter(PlayerChampionData::strength),
            Codec.INT.optionalFieldOf("defence", 0).forGetter(PlayerChampionData::defence),
            Codec.INT.optionalFieldOf("dexterity", 0).forGetter(PlayerChampionData::dexterity),
            Codec.BOOL.optionalFieldOf("needsStatScreen", false).forGetter(PlayerChampionData::needsStatScreen)
    ).apply(instance, PlayerChampionData::new));

    // Network codec — syncs this data to the owning client so the HUD can read it.
    public static final PacketCodec<RegistryByteBuf, PlayerChampionData> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, PlayerChampionData::champLevel,
            PacketCodecs.VAR_INT, PlayerChampionData::champXP,
            PacketCodecs.VAR_INT, PlayerChampionData::statPoints,
            PacketCodecs.VAR_INT, PlayerChampionData::vitality,
            PacketCodecs.VAR_INT, PlayerChampionData::strength,
            PacketCodecs.VAR_INT, PlayerChampionData::defence,
            PacketCodecs.VAR_INT, PlayerChampionData::dexterity,
            PacketCodecs.BOOLEAN, PlayerChampionData::needsStatScreen,
            PlayerChampionData::new
    );

    // The attachment: persistent = saved to disk, copyOnDeath = survives respawn,
    // initializer = default for new players, syncWith = mirror to the owning client.
    public static final AttachmentType<PlayerChampionData> ATTACHMENT = AttachmentRegistry.create(
            Identifier.of(SwordShieldAndBow.MOD_ID, "champion_data"),
            builder -> builder
                    .persistent(CODEC)
                    .copyOnDeath()
                    .initializer(() -> DEFAULT)
                    .syncWith(PACKET_CODEC, AttachmentSyncPredicate.targetOnly())
    );

    public static void initialize() {
        // empty — just here so the mod init can load this class and register the attachment
    }

    public PlayerChampionData withChampLevel(int value) {
        return new PlayerChampionData(value, champXP, statPoints, vitality, strength, defence, dexterity, needsStatScreen);
    }

    public PlayerChampionData withChampXP(int value) {
        return new PlayerChampionData(champLevel, value, statPoints, vitality, strength, defence, dexterity, needsStatScreen);
    }

    public PlayerChampionData withStatPoints(int value) {
        return new PlayerChampionData(champLevel, champXP, value, vitality, strength, defence, dexterity, needsStatScreen);
    }

    public PlayerChampionData withVitality(int value) {
        return new PlayerChampionData(champLevel, champXP, statPoints, value, strength, defence, dexterity, needsStatScreen);
    }

    public PlayerChampionData withStrength(int value) {
        return new PlayerChampionData(champLevel, champXP, statPoints, vitality, value, defence, dexterity, needsStatScreen);
    }

    public PlayerChampionData withDefence(int value) {
        return new PlayerChampionData(champLevel, champXP, statPoints, vitality, strength, value, dexterity, needsStatScreen);
    }

    public PlayerChampionData withDexterity(int value) {
        return new PlayerChampionData(champLevel, champXP, statPoints, vitality, strength, defence, value, needsStatScreen);
    }

    public PlayerChampionData withNeedsStatScreen(boolean value) {
        return new PlayerChampionData(champLevel, champXP, statPoints, vitality, strength, defence, dexterity, value);
    }
}
