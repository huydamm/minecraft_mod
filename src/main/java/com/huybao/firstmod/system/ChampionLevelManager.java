package com.huybao.firstmod.system;

import com.huybao.firstmod.data.PlayerChampionData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

// Champion leveling: XP from kills, level-ups, stat points, and the "needs screen" flag.
public final class ChampionLevelManager {

    public static final int STAT_POINTS_PER_LEVEL = 1;
    public static final int STAT_SCREEN_LEVEL_INTERVAL = 3;

    // Base curve is level^2 * 100; divide by this to make leveling a bit quicker.
    private static final double XP_DIVISOR = 1.5;

    public static final int NORMAL_KILL_XP = 50;
    public static final int ELITE_KILL_XP = 150;

    private ChampionLevelManager() {
    }

    // XP needed to reach `level`. Next level costs xpForLevel(current + 1) — using +1
    // keeps level 0 from costing nothing (which would loop forever).
    public static int xpForLevel(int level) {
        return (int) (level * level * 100 / XP_DIVISOR);
    }

    // Adds XP and rolls any level-ups: each level gives stat points, every few levels
    // flags that a stat screen is owed.
    public static void addXP(PlayerEntity player, int amount) {
        if (amount <= 0) {
            return;
        }

        PlayerChampionData data = player.getAttachedOrCreate(PlayerChampionData.ATTACHMENT);

        int level = data.champLevel();
        int xp = data.champXP() + amount;
        int statPoints = data.statPoints();
        boolean needsStatScreen = data.needsStatScreen();

        // level up as many times as the XP covers
        while (xp >= xpForLevel(level + 1)) {
            xp -= xpForLevel(level + 1);
            level++;
            statPoints += STAT_POINTS_PER_LEVEL;

            if (level % STAT_SCREEN_LEVEL_INTERVAL == 0) {
                needsStatScreen = true;
            }
        }

        // Points just sit there until the player opens the screen themselves — nothing pops up.
        PlayerChampionData updated = new PlayerChampionData(
                level, xp, statPoints,
                data.vitality(), data.strength(), data.defence(), data.dexterity(),
                needsStatScreen);

        player.setAttached(PlayerChampionData.ATTACHMENT, updated);
    }

    // Hook mob deaths so the killer gets XP. Call from onInitialize().
    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            // only mobs count (skip players, armor stands, etc.)
            if (!(entity instanceof MobEntity)) {
                return;
            }

            Entity attacker = damageSource.getAttacker();
            if (attacker instanceof PlayerEntity player) {
                // named/elite mobs are worth more
                int xp = entity.hasCustomName() ? ELITE_KILL_XP : NORMAL_KILL_XP;
                addXP(player, xp);
            }
        });
    }
}
