package com.huybao.firstmod.client;

import com.huybao.firstmod.SwordShieldAndBow;
import com.huybao.firstmod.client.hud.ChampionHudOverlay;
import com.huybao.firstmod.client.screen.StatSheetScreen;
import com.huybao.firstmod.network.OpenStatScreenPayload;
import com.huybao.firstmod.network.RequestStatScreenPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SwordShieldAndBowClient implements ClientModInitializer {

	// Standard inventory GUI dimensions, used to place the button relative to the panel.
	private static final int INVENTORY_WIDTH = 176;
	private static final int INVENTORY_HEIGHT = 166;

	@Override
	public void onInitializeClient() {
		// When the server asks us to, open the stat sheet (on the client/render thread).
		ClientPlayNetworking.registerGlobalReceiver(OpenStatScreenPayload.ID, (payload, context) ->
				context.client().execute(() ->
						context.client().setScreen(new StatSheetScreen(payload))));

		// Add a "Stats" button to the player inventory that opens the stat sheet. The client
		// doesn't hold the champion data, so the button just asks the server to open the screen.
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

		// Draw a small champion-XP progress bar on the HUD. addLast → rendered on top of the
		// vanilla bars so nothing paints over it.
		HudElementRegistry.addLast(
				Identifier.of(SwordShieldAndBow.MOD_ID, "champion_xp_bar"), new ChampionHudOverlay());
	}
}
