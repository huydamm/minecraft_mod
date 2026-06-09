package com.huybao.firstmod.system;

import com.huybao.firstmod.SwordShieldAndBow;
import com.huybao.firstmod.data.PlayerChampionData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Turns a player's allocated stats into actual gameplay bonuses:
 * <ul>
 *   <li><b>Vitality</b> — +1/4 heart (0.5 max health) per point.</li>
 *   <li><b>Strength</b> — Strength effect, one tier per 4 points (up to V).</li>
 *   <li><b>Defence</b> — Resistance effect, one tier per 4 points (up to V).</li>
 *   <li><b>Dexterity</b> — Haste and Speed effects, one tier per 4 points (up to V).</li>
 * </ul>
 *
 * Bonuses are re-derived from the player's data on a periodic server tick (and immediately
 * after an allocation). Doing it on a tick — rather than only on respawn/join — reliably
 * survives death, dimension changes and {@code /effect clear}. The apply is idempotent, so
 * it only changes effects/attributes that are actually wrong (no flicker).
 */
public final class ChampionStatEffects {

    private static final double HEALTH_PER_VITALITY = 0.5; // 1/4 heart
    private static final int POINTS_PER_TIER = 4;
    private static final int MAX_TIER = 5; // level V
    private static final int REAPPLY_INTERVAL_TICKS = 20; // once per second

    private static final Identifier VITALITY_HEALTH_ID =
            Identifier.of(SwordShieldAndBow.MOD_ID, "vitality_health");

    private ChampionStatEffects() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % REAPPLY_INTERVAL_TICKS != 0) {
                return;
            }
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                apply(player);
            }
        });
    }

    /** Re-derives all stat bonuses from the player's champion data (idempotent). */
    public static void apply(ServerPlayerEntity player) {
        PlayerChampionData data = player.getAttachedOrCreate(PlayerChampionData.ATTACHMENT);

        applyVitality(player, data.vitality());
        applyTieredEffect(player, StatusEffects.STRENGTH, data.strength());
        applyTieredEffect(player, StatusEffects.RESISTANCE, data.defence());
        applyTieredEffect(player, StatusEffects.HASTE, data.dexterity());
        applyTieredEffect(player, StatusEffects.SPEED, data.dexterity());
    }

    private static void applyVitality(ServerPlayerEntity player, int vitality) {
        EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        double bonus = vitality * HEALTH_PER_VITALITY;
        EntityAttributeModifier existing = maxHealth.getModifier(VITALITY_HEALTH_ID);

        if (bonus <= 0) {
            if (existing != null) {
                maxHealth.removeModifier(VITALITY_HEALTH_ID);
            }
            return;
        }
        if (existing != null && existing.value() == bonus) {
            return; // already correct
        }
        if (existing != null) {
            maxHealth.removeModifier(VITALITY_HEALTH_ID);
        }
        maxHealth.addPersistentModifier(new EntityAttributeModifier(
                VITALITY_HEALTH_ID, bonus, EntityAttributeModifier.Operation.ADD_VALUE));

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void applyTieredEffect(ServerPlayerEntity player, RegistryEntry<StatusEffect> effect, int statValue) {
        int tier = Math.min(statValue / POINTS_PER_TIER, MAX_TIER); // 4 pts = I ... 20 pts = V
        StatusEffectInstance current = player.getStatusEffect(effect);

        if (tier <= 0) {
            if (current != null) {
                player.removeStatusEffect(effect);
            }
            return;
        }

        int amplifier = tier - 1; // amplifier 0 == level I
        if (current != null && current.getAmplifier() == amplifier && current.isInfinite()) {
            return; // already correct
        }
        player.addStatusEffect(new StatusEffectInstance(
                effect, StatusEffectInstance.INFINITE, amplifier,
                false, false, true)); // ambient=false, showParticles=false, showIcon=true
    }
}
