package com.huybao.firstmod.client.screen;

import com.huybao.firstmod.network.ChallengePlayerPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

// Pops up when you right-click another player. Just a "Duel" option for now;
// more options (trade, party, etc.) can hang off the same panel later.
public class PlayerInteractScreen extends Screen {

    private static final int PANEL_WIDTH = 180;
    private static final int PANEL_HEIGHT = 120;
    private static final int BUTTON_WIDTH = 140;
    private static final int BUTTON_HEIGHT = 20;

    private static final int COLOR_PANEL = 0xE0101018;
    private static final int COLOR_BORDER = 0xFF3F3F8F;
    private static final int COLOR_TEXT = 0xFFFFFFFF;

    private final int targetEntityId;
    private final String targetName;

    private int panelLeft;
    private int panelTop;

    public PlayerInteractScreen(int targetEntityId, String targetName) {
        super(Text.literal("Player Options"));
        this.targetEntityId = targetEntityId;
        this.targetName = targetName;
    }

    @Override
    protected void init() {
        panelLeft = (this.width - PANEL_WIDTH) / 2;
        panelTop = (this.height - PANEL_HEIGHT) / 2;

        int buttonX = panelLeft + (PANEL_WIDTH - BUTTON_WIDTH) / 2;
        int firstY = panelTop + 44;

        addDrawableChild(ButtonWidget.builder(Text.literal("Duel"), b -> duel())
                .dimensions(buttonX, firstY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        // More options will go here later.

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> this.close())
                .dimensions(buttonX, panelTop + PANEL_HEIGHT - 28, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private void duel() {
        ClientPlayNetworking.send(new ChallengePlayerPayload(targetEntityId));
        this.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(panelLeft - 1, panelTop - 1, panelLeft + PANEL_WIDTH + 1, panelTop + PANEL_HEIGHT + 1, COLOR_BORDER);
        context.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, COLOR_PANEL);

        int centerX = panelLeft + PANEL_WIDTH / 2;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(targetName), centerX, panelTop + 12, COLOR_TEXT);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Choose an option"), centerX, panelTop + 26, COLOR_TEXT);

        super.render(context, mouseX, mouseY, delta);
    }
}
