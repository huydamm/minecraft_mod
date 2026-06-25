package com.huybao.firstmod;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.KilledByPlayerLootCondition;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;

import java.util.HashSet;
import java.util.Set;

public class ModLootTableModifiers {

    // 75% chance
    private static final float DROP_CHANCE = 0.75f;

    public static void modifyLootTables() {

        Set<RegistryKey<LootTable>> hostileLootTables = collectHostileLootTables();

        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (source.isBuiltin() && hostileLootTables.contains(key)) {
                LootPool.Builder pool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        // Only drop when players kill mobs
                        .conditionally(KilledByPlayerLootCondition.builder())
                        .conditionally(RandomChanceLootCondition.builder(DROP_CHANCE))
                        .with(ItemEntry.builder(ModItems.GOLD_COIN)
                                .apply(SetCountLootFunction.builder(
                                        UniformLootNumberProvider.create(1.0f, 3.0f))));

                tableBuilder.pool(pool);
            }
        });
    }

    private static Set<RegistryKey<LootTable>> collectHostileLootTables() {
        Set<RegistryKey<LootTable>> set = new HashSet<>();

        Registries.ENTITY_TYPE.forEach(entityType -> {
            if (entityType.getSpawnGroup() == SpawnGroup.MONSTER) {
                entityType.getLootTableKey().ifPresent(set::add);
            }
        });

        return set;
    }
}
