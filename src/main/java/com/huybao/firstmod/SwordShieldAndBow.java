package com.huybao.firstmod;

import com.huybao.firstmod.command.ChampionDebugCommand;
import com.huybao.firstmod.command.DuelCommand;
import com.huybao.firstmod.data.PlayerChampionData;
import com.huybao.firstmod.network.ModNetworking;
import com.huybao.firstmod.system.ChampionLevelManager;
import com.huybao.firstmod.system.ChampionStatEffects;
import com.huybao.firstmod.system.ClassManager;
import com.huybao.firstmod.system.DuelManager;
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
		ClassManager.register();              // first-join class selection + class modifiers
		DuelManager.register();               // PvP off + /duel system
		ChampionDebugCommand.register();      // TEMP: /champion test command
		DuelCommand.register();               // /duel challenge|accept|deny

		ModItems.initialize();
		ModLootTableModifiers.modifyLootTables();
	}
}