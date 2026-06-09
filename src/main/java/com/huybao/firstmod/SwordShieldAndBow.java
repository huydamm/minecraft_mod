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

	// Logger named after the mod id, so it's clear who logged what.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PlayerChampionData.initialize();      // the champion data attachment
		ModNetworking.registerPayloads();     // packets (both sides)
		ModNetworking.registerServerSide();   // + their server handlers
		ChampionLevelManager.register();      // XP from mob kills
		ChampionStatEffects.register();       // keep stat bonuses applied
		ChampionDebugCommand.register();      // TEMP: /champion test command

		ModItems.initialize();
		ModLootTableModifiers.modifyLootTables();
	}
}