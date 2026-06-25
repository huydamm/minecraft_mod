package com.huybao.firstmod.system;

import com.huybao.firstmod.SwordShieldAndBow;
import com.huybao.firstmod.data.PlayerChampionData;
import com.huybao.firstmod.data.PlayerClass;
import com.huybao.firstmod.mixin.PersistentProjectileEntityAccessor;
import com.huybao.firstmod.network.OpenClassScreenS2CPacket;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

// First-join class selection: prompts the screen on join, then applies the chosen class's
// permanent stat modifiers. Modifiers are keyed by Identifier (1.21 dropped the old UUID keys)
// and added as persistent modifiers, so they save with the player's attribute data.
public final class ClassManager {

    // One stable key per stat per class, so we can detect (and never double-apply) them.
    private static final Identifier KNIGHT_DAMAGE = id("class_knight_attack_damage");
    private static final Identifier KNIGHT_HEALTH = id("class_knight_max_health");
    private static final Identifier KNIGHT_ARMOR = id("class_knight_armor");
    private static final Identifier KNIGHT_TOUGHNESS = id("class_knight_armor_toughness");
    private static final Identifier ARCHER_SPEED = id("class_archer_movement_speed");
    private static final Identifier ARCHER_HEALTH = id("class_archer_max_health");
    private static final Identifier GIANT_HEALTH = id("class_giant_max_health");
    private static final Identifier GIANT_SLOW = id("class_giant_movement_speed");
    private static final Identifier GIANT_ARMOR = id("class_giant_armor");
    private static final Identifier GIANT_DAMAGE = id("class_giant_attack_damage");
    private static final Identifier GIANT_KNOCKBACK = id("class_giant_knockback_resistance");
    private static final Identifier GIANT_SIZE = id("class_giant_scale");

    // Flat bonus added to every projectile an Archer shoots. Not an attribute — applied
    // to the projectile at spawn. The tag stops it being re-applied if the arrow is
    // saved and reloaded mid-flight.
    private static final double ARCHER_PROJECTILE_BONUS = 4.0;
    private static final String ARCHER_PROJECTILE_TAG = "ssab_archer_projectile";

    private ClassManager() {
    }

    private static Identifier id(String path) {
        return Identifier.of(SwordShieldAndBow.MOD_ID, path);
    }

    // Prompt the screen the first time a player joins (and any later join until they've chosen),
    // and boost the damage of projectiles fired by Archers.
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            PlayerChampionData data = player.getAttachedOrCreate(PlayerChampionData.ATTACHMENT);
            if (!data.hasChosenClass()) {
                ServerPlayNetworking.send(player, new OpenClassScreenS2CPacket());
            }
        });

        // Fires once when an entity is added to the world — including a freshly shot arrow/bolt,
        // by which point its owner and base (enchanted) damage are already set.
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof PersistentProjectileEntity projectile)
                    || projectile.getCommandTags().contains(ARCHER_PROJECTILE_TAG)) {
                return;
            }
            if (projectile.getOwner() instanceof ServerPlayerEntity shooter
                    && shooter.getAttachedOrCreate(PlayerChampionData.ATTACHMENT).playerClass() == PlayerClass.ARCHER) {
                double current = ((PersistentProjectileEntityAccessor) projectile).getDamage();
                projectile.setDamage(current + ARCHER_PROJECTILE_BONUS);
                projectile.addCommandTag(ARCHER_PROJECTILE_TAG);
            }
        });
    }

    // Server-authoritative handler for a SelectClassC2SPacket.
    public static void choose(ServerPlayerEntity player, PlayerClass chosen) {
        if (chosen == PlayerClass.NONE) {
            return; // not a real pick — ignore (e.g. a spoofed packet)
        }

        PlayerChampionData data = player.getAttachedOrCreate(PlayerChampionData.ATTACHMENT);
        if (data.hasChosenClass()) {
            return; // already chosen — ignore so a class can't be re-rolled / stacked
        }

        PlayerChampionData updated = data
                .withPlayerClass(chosen)
                .withHasChosenClass(true)
                .withXpPenalty(chosen == PlayerClass.VAGABOND);
        player.setAttached(PlayerChampionData.ATTACHMENT, updated);

        applyModifiers(player, chosen);
    }

    private static void applyModifiers(ServerPlayerEntity player, PlayerClass playerClass) {
        switch (playerClass) {
            case KNIGHT -> {
                add(player, EntityAttributes.ATTACK_DAMAGE, KNIGHT_DAMAGE, 2.0,
                        EntityAttributeModifier.Operation.ADD_VALUE);
                add(player, EntityAttributes.MAX_HEALTH, KNIGHT_HEALTH, 4.0, // +2 hearts
                        EntityAttributeModifier.Operation.ADD_VALUE);
                add(player, EntityAttributes.ARMOR, KNIGHT_ARMOR, 3.0,
                        EntityAttributeModifier.Operation.ADD_VALUE);
                add(player, EntityAttributes.ARMOR_TOUGHNESS, KNIGHT_TOUGHNESS, 3.0,
                        EntityAttributeModifier.Operation.ADD_VALUE);
            }
            case ARCHER -> {
                add(player, EntityAttributes.MOVEMENT_SPEED, ARCHER_SPEED, 0.30,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                // +4 projectile damage is handled at projectile spawn (see register()), not here.
                add(player, EntityAttributes.MAX_HEALTH, ARCHER_HEALTH, -4.0, // -2 hearts
                        EntityAttributeModifier.Operation.ADD_VALUE);
            }
            case GIANT -> {
                add(player, EntityAttributes.MAX_HEALTH, GIANT_HEALTH, 16.0, // +8 hearts
                        EntityAttributeModifier.Operation.ADD_VALUE);
                add(player, EntityAttributes.MOVEMENT_SPEED, GIANT_SLOW, -0.20,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                add(player, EntityAttributes.ARMOR, GIANT_ARMOR, 5.0,
                        EntityAttributeModifier.Operation.ADD_VALUE);
                add(player, EntityAttributes.ATTACK_DAMAGE, GIANT_DAMAGE, 3.0,
                        EntityAttributeModifier.Operation.ADD_VALUE);
                add(player, EntityAttributes.KNOCKBACK_RESISTANCE, GIANT_KNOCKBACK, 0.20,
                        EntityAttributeModifier.Operation.ADD_VALUE);
                // Base scale is 1.0; +0.5 makes the model (and hitbox) 1.5x size.
                add(player, EntityAttributes.SCALE, GIANT_SIZE, 0.5,
                        EntityAttributeModifier.Operation.ADD_VALUE);
            }
            default -> {
                // VAGABOND (and NONE): no attribute changes. The XP penalty lives on the data flag.
            }
        }

        // If max health dropped (archer), don't leave them above the new cap.
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // Add a persistent modifier, skipping it if its key is already present (the double-apply guard).
    private static void add(ServerPlayerEntity player, RegistryEntry<EntityAttribute> attribute,
                            Identifier key, double value, EntityAttributeModifier.Operation operation) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) {
            return;
        }
        if (instance.getModifier(key) != null) {
            return; // already applied — don't stack it
        }
        instance.addPersistentModifier(new EntityAttributeModifier(key, value, operation));
    }
}
