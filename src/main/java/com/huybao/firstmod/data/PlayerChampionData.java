package com.huybao.firstmod.data;

import com.huybao.firstmod.SwordShieldAndBow;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;

// Per-player champion data: level, XP, stat points, the 4 stats, plus the chosen class.
public record PlayerChampionData(
        int champLevel,
        int champXP,
        int statPoints,
        int vitality,
        int strength,
        int defence,
        int dexterity,
        boolean needsStatScreen,
        PlayerClass playerClass,
        boolean hasChosenClass,
        boolean xpBonus
) {
    public static final PlayerChampionData DEFAULT =
            new PlayerChampionData(0, 0, 0, 0, 0, 0, 0, false, PlayerClass.NONE, false, false);

    // Save codec. optionalFieldOf means old saves / new fields just default instead of crashing.
    public static final Codec<PlayerChampionData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("champLevel", 0).forGetter(PlayerChampionData::champLevel),
            Codec.INT.optionalFieldOf("champXP", 0).forGetter(PlayerChampionData::champXP),
            Codec.INT.optionalFieldOf("statPoints", 0).forGetter(PlayerChampionData::statPoints),
            Codec.INT.optionalFieldOf("vitality", 0).forGetter(PlayerChampionData::vitality),
            Codec.INT.optionalFieldOf("strength", 0).forGetter(PlayerChampionData::strength),
            Codec.INT.optionalFieldOf("defence", 0).forGetter(PlayerChampionData::defence),
            Codec.INT.optionalFieldOf("dexterity", 0).forGetter(PlayerChampionData::dexterity),
            Codec.BOOL.optionalFieldOf("needsStatScreen", false).forGetter(PlayerChampionData::needsStatScreen),
            PlayerClass.CODEC.optionalFieldOf("playerClass", PlayerClass.NONE).forGetter(PlayerChampionData::playerClass),
            Codec.BOOL.optionalFieldOf("hasChosenClass", false).forGetter(PlayerChampionData::hasChosenClass),
            Codec.BOOL.optionalFieldOf("xpBonus", false).forGetter(PlayerChampionData::xpBonus)
    ).apply(instance, PlayerChampionData::new));

    public static final PacketCodec<RegistryByteBuf, PlayerChampionData> PACKET_CODEC =
            PacketCodec.ofStatic(PlayerChampionData::write, PlayerChampionData::read);

    private static void write(RegistryByteBuf buf, PlayerChampionData data) {
        buf.writeVarInt(data.champLevel);
        buf.writeVarInt(data.champXP);
        buf.writeVarInt(data.statPoints);
        buf.writeVarInt(data.vitality);
        buf.writeVarInt(data.strength);
        buf.writeVarInt(data.defence);
        buf.writeVarInt(data.dexterity);
        buf.writeBoolean(data.needsStatScreen);
        buf.writeVarInt(data.playerClass.ordinal());
        buf.writeBoolean(data.hasChosenClass);
        buf.writeBoolean(data.xpBonus);
    }

    private static PlayerChampionData read(RegistryByteBuf buf) {
        int champLevel = buf.readVarInt();
        int champXP = buf.readVarInt();
        int statPoints = buf.readVarInt();
        int vitality = buf.readVarInt();
        int strength = buf.readVarInt();
        int defence = buf.readVarInt();
        int dexterity = buf.readVarInt();
        boolean needsStatScreen = buf.readBoolean();
        PlayerClass playerClass = PlayerClass.byId(buf.readVarInt());
        boolean hasChosenClass = buf.readBoolean();
        boolean xpBonus = buf.readBoolean();
        return new PlayerChampionData(champLevel, champXP, statPoints, vitality, strength, defence,
                dexterity, needsStatScreen, playerClass, hasChosenClass, xpBonus);
    }

    // The attachment: persistent = saved to disk, copyOnDeath = survives respawn,
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
        return new PlayerChampionData(value, champXP, statPoints, vitality, strength, defence, dexterity, needsStatScreen, playerClass, hasChosenClass, xpBonus);
    }

    public PlayerChampionData withChampXP(int value) {
        return new PlayerChampionData(champLevel, value, statPoints, vitality, strength, defence, dexterity, needsStatScreen, playerClass, hasChosenClass, xpBonus);
    }

    public PlayerChampionData withStatPoints(int value) {
        return new PlayerChampionData(champLevel, champXP, value, vitality, strength, defence, dexterity, needsStatScreen, playerClass, hasChosenClass, xpBonus);
    }

    public PlayerChampionData withVitality(int value) {
        return new PlayerChampionData(champLevel, champXP, statPoints, value, strength, defence, dexterity, needsStatScreen, playerClass, hasChosenClass, xpBonus);
    }

    public PlayerChampionData withStrength(int value) {
        return new PlayerChampionData(champLevel, champXP, statPoints, vitality, value, defence, dexterity, needsStatScreen, playerClass, hasChosenClass, xpBonus);
    }

    public PlayerChampionData withDefence(int value) {
        return new PlayerChampionData(champLevel, champXP, statPoints, vitality, strength, value, dexterity, needsStatScreen, playerClass, hasChosenClass, xpBonus);
    }

    public PlayerChampionData withDexterity(int value) {
        return new PlayerChampionData(champLevel, champXP, statPoints, vitality, strength, defence, value, needsStatScreen, playerClass, hasChosenClass, xpBonus);
    }

    public PlayerChampionData withNeedsStatScreen(boolean value) {
        return new PlayerChampionData(champLevel, champXP, statPoints, vitality, strength, defence, dexterity, value, playerClass, hasChosenClass, xpBonus);
    }

    public PlayerChampionData withPlayerClass(PlayerClass value) {
        return new PlayerChampionData(champLevel, champXP, statPoints, vitality, strength, defence, dexterity, needsStatScreen, value, hasChosenClass, xpBonus);
    }

    public PlayerChampionData withHasChosenClass(boolean value) {
        return new PlayerChampionData(champLevel, champXP, statPoints, vitality, strength, defence, dexterity, needsStatScreen, playerClass, value, xpBonus);
    }

    public PlayerChampionData withXpBonus(boolean value) {
        return new PlayerChampionData(champLevel, champXP, statPoints, vitality, strength, defence, dexterity, needsStatScreen, playerClass, hasChosenClass, value);
    }
}
