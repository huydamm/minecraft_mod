package com.huybao.firstmod;

import com.huybao.firstmod.command.ChampionDebugCommand;
import com.huybao.firstmod.data.PlayerChampionData;
import com.huybao.firstmod.network.ModNetworking;
import com.huybao.firstmod.system.ChampionLevelManager;
import com.huybao.firstmod.system.ChampionStatEffects;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwordShieldAndBow implements ModInitializer {
	public static final String MOD_ID = "sword-shield-and-bow";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Register the per-player champion data attachment (persists through death and restart).
		PlayerChampionData.initialize();
		// Register custom payloads (both sides) and server-side packet handling.
		ModNetworking.registerPayloads();
		ModNetworking.registerServerSide();
		// Award champion XP on mob kills and handle level-ups.
		ChampionLevelManager.register();
		// Apply stat bonuses (max health / effects) on join and respawn.
		ChampionStatEffects.register();
		// TEMPORARY: /champion debug commands for testing (remove when done).
		ChampionDebugCommand.register();

		ModItems.initialize();
		ModLootTableModifiers.modifyLootTables();
	}
}