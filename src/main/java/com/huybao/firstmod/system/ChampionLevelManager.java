package com.huybao.firstmod.system;

import com.huybao.firstmod.data.PlayerChampionData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Drives the champion leveling system: awards XP on mob kills, handles level-ups,
 * grants stat points, and flags when the player is owed a stat-allocation screen.
 */
public final class ChampionLevelManager {

    public static final int STAT_POINTS_PER_LEVEL = 1;
    public static final int STAT_SCREEN_LEVEL_INTERVAL = 3;

    // The base XP curve (level^2 * 100) divided by this to make leveling faster.
    private static final double XP_DIVISOR = 1.5;

    public static final int NORMAL_KILL_XP = 50;
    public static final int ELITE_KILL_XP = 150;

    private ChampionLevelManager() {
    }

    /**
     * XP required to reach {@code level} from the previous one: {@code level * level * 100}.
     * Advancing from your current level therefore costs {@code xpForLevel(currentLevel + 1)}
     * (this avoids the degenerate zero-cost case at level 0).
     */
    public static int xpForLevel(int level) {
        return (int) (level * level * 100 / XP_DIVISOR);
    }

    /**
     * Adds XP to a player's champion data, applying any resulting level-ups. Each level-up
     * grants {@link #STAT_POINTS_PER_LEVEL} stat points, and every
     * {@link #STAT_SCREEN_LEVEL_INTERVAL} levels sets {@code needsStatScreen = true}.
     */
    public static void addXP(PlayerEntity player, int amount) {
        if (amount <= 0) {
            return;
        }

        PlayerChampionData data = player.getAttachedOrCreate(PlayerChampionData.ATTACHMENT);

        int level = data.champLevel();
        int xp = data.champXP() + amount;
        int statPoints = data.statPoints();
        boolean needsStatScreen = data.needsStatScreen();

        // Spend XP on as many level-ups as it covers.
        while (xp >= xpForLevel(level + 1)) {
            xp -= xpForLevel(level + 1);
            level++;
            statPoints += STAT_POINTS_PER_LEVEL;

            if (level % STAT_SCREEN_LEVEL_INTERVAL == 0) {
                needsStatScreen = true;
            }
        }

        // Points are simply banked; the player opens the stat screen when they choose to
        // (via the inventory button). No screen is forced open on level-up.
        PlayerChampionData updated = new PlayerChampionData(
                level, xp, statPoints,
                data.vitality(), data.strength(), data.defence(), data.dexterity(),
                needsStatScreen);

        player.setAttached(PlayerChampionData.ATTACHMENT, updated);
    }

    /** Registers the "mob death grants champion XP to its killer" hook. Call from onInitialize(). */
    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            // Only mob deaths grant champion XP (ignore players, armor stands, etc.).
            if (!(entity instanceof MobEntity)) {
                return;
            }

            Entity attacker = damageSource.getAttacker();
            if (attacker instanceof PlayerEntity player) {
                // Named/elite mobs are worth more.
                int xp = entity.hasCustomName() ? ELITE_KILL_XP : NORMAL_KILL_XP;
                addXP(player, xp);
            }
        });
    }
}
