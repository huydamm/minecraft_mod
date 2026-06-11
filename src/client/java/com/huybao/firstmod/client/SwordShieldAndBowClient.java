package com.huybao.firstmod.client;

import com.huybao.firstmod.SwordShieldAndBow;
import com.huybao.firstmod.client.hud.ChampionHudOverlay;
import com.huybao.firstmod.client.screen.PlayerInteractScreen;
import com.huybao.firstmod.client.screen.StatSheetScreen;
import com.huybao.firstmod.network.OpenStatScreenPayload;
import com.huybao.firstmod.network.RequestStatScreenPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class SwordShieldAndBowClient implements ClientModInitializer {

	// Vanilla inventory size, for placing the button relative to it.
	private static final int INVENTORY_WIDTH = 176;
	private static final int INVENTORY_HEIGHT = 166;

	@Override
	public void onInitializeClient() {
		// server says open the stat sheet -> do it on the client thread
		ClientPlayNetworking.registerGlobalReceiver(OpenStatScreenPayload.ID, (payload, context) ->
				context.client().execute(() ->
						context.client().setScreen(new StatSheetScreen(payload))));

		// "Stats" button in the inventory. The client has no data, so it just asks the server to open.
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof InventoryScreen)) {
				return;
			}
			int left = (scaledWidth - INVENTORY_WIDTH) / 2;
			int top = (scaledHeight - INVENTORY_HEIGHT) / 2;

			ButtonWidget statsButton = ButtonWidget.builder(Text.literal("Stats"),
							b -> ClientPlayNetworking.send(new RequestStatScreenPayload()))
					.dimensions(left + INVENTORY_WIDTH - 60, top - 22, 60, 20)
					.build();

			Screens.getButtons(screen).add(statsButton);
		});

		// Right-clicking another player opens an options panel (Duel, etc.).
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			// one hand only, so we don't open the panel twice per click
			if (hand != Hand.MAIN_HAND || !(entity instanceof PlayerEntity) || entity == player) {
				return ActionResult.PASS;
			}
			net.minecraft.client.MinecraftClient.getInstance().setScreen(
					new PlayerInteractScreen(entity.getId(), entity.getName().getString()));
			return ActionResult.SUCCESS; // consume so vanilla doesn't also handle the click
		});

		// champion XP bar on the HUD. addLast = drawn on top so nothing covers it.
		HudElementRegistry.addLast(
				Identifier.of(SwordShieldAndBow.MOD_ID, "champion_xp_bar"), new ChampionHudOverlay());
	}
}
