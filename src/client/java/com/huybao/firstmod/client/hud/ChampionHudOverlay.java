package com.huybao.firstmod.client.hud;

import com.huybao.firstmod.data.PlayerChampionData;
import com.huybao.firstmod.system.ChampionLevelManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

// Top-left HUD widget: champion level, an XP bar, and the XP count.
// Reads the synced data attachment. Move it with X / Y.
public class ChampionHudOverlay implements HudElement {

    private static final int X = 6;
    private static final int Y = 6;
    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 5;

    // ARGB colors.
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_SUBTEXT = 0xFFB0B0B0;
    private static final int COLOR_BORDER = 0xFF000000;
    private static final int COLOR_BG = 0xC0202020;
    private static final int COLOR_FILL = 0xFF35C8FF; // cyan

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        PlayerChampionData data = player.getAttached(PlayerChampionData.ATTACHMENT);
        if (data == null) {
            data = PlayerChampionData.DEFAULT; // not synced yet — draw an empty bar
        }

        int needed = ChampionLevelManager.xpForLevel(data.champLevel() + 1);
        float fraction = needed > 0 ? Math.min(1.0f, (float) data.champXP() / needed) : 0.0f;

        // Level label.
        context.drawTextWithShadow(client.textRenderer,
                Text.literal("Champion Lv " + data.champLevel()), X, Y, COLOR_TEXT);

        // Progress bar.
        int barTop = Y + 11;
        int barBottom = barTop + BAR_HEIGHT;
        context.fill(X - 1, barTop - 1, X + BAR_WIDTH + 1, barBottom + 1, COLOR_BORDER);
        context.fill(X, barTop, X + BAR_WIDTH, barBottom, COLOR_BG);
        context.fill(X, barTop, X + Math.round(BAR_WIDTH * fraction), barBottom, COLOR_FILL);

        // XP count under the bar.
        context.drawTextWithShadow(client.textRenderer,
                Text.literal(data.champXP() + " / " + needed + " XP"), X, barBottom + 3, COLOR_SUBTEXT);
    }
}
