package com.huybao.firstmod.client.screen;

import com.huybao.firstmod.network.AllocateStatsPayload;
import com.huybao.firstmod.network.OpenStatScreenPayload;
import com.huybao.firstmod.system.ChampionLevelManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Stat-allocation screen. Shows the player's champion level, an XP progress bar toward the next
 * level, and the remaining stat points, with {@code +}/{@code -} buttons per stat. Points are
 * spent client-side for preview; on Confirm the chosen allocation is sent to the server (which
 * validates and applies it). While points remain unspent, ESC will not close the screen.
 *
 * <p>Stat order: Vitality, Strength, Dexterity, Defence. Note that DrawContext colors are
 * ARGB, so all colors include a 0xFF alpha byte (otherwise the drawing is invisible).</p>
 */
public class StatSheetScreen extends Screen {

    private static final String[] STAT_NAMES = {"Vitality", "Strength", "Dexterity", "Defence"};

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 216;
    private static final int ROW_HEIGHT = 26;

    // ARGB colors (alpha matters — 0x00RRGGBB renders invisible).
    private static final int COLOR_PANEL = 0xE0101018;
    private static final int COLOR_BORDER = 0xFF3F3F8F;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_POINTS = 0xFFFFD700;
    private static final int COLOR_BAR_BG = 0xFF000000;
    private static final int COLOR_BAR_FILL = 0xFF4FC34F;

    private final int championLevel;
    private final int currentXP;
    private final int[] base = new int[4];   // [vit, str, dex, def]
    private final int[] added = new int[4];
    private int remainingPoints;

    private int panelLeft;
    private int panelTop;
    private int rowsTop;

    private final ButtonWidget[] plusButtons = new ButtonWidget[4];
    private final ButtonWidget[] minusButtons = new ButtonWidget[4];

    public StatSheetScreen(OpenStatScreenPayload data) {
        super(Text.literal("Stat Sheet"));
        this.championLevel = data.championLevel();
        this.currentXP = data.champXP();
        this.remainingPoints = data.statPoints();
        this.base[0] = data.vitality();
        this.base[1] = data.strength();
        this.base[2] = data.dexterity();
        this.base[3] = data.defence();
    }

    @Override
    protected void init() {
        panelLeft = (this.width - PANEL_WIDTH) / 2;
        panelTop = (this.height - PANEL_HEIGHT) / 2;
        rowsTop = panelTop + 78;

        int minusX = panelLeft + PANEL_WIDTH - 56;
        int plusX = panelLeft + PANEL_WIDTH - 30;

        for (int i = 0; i < 4; i++) {
            final int index = i;
            int y = rowsTop + i * ROW_HEIGHT - 4;

            ButtonWidget minus = ButtonWidget.builder(Text.literal("-"), b -> onMinus(index))
                    .dimensions(minusX, y, 20, 20).build();
            ButtonWidget plus = ButtonWidget.builder(Text.literal("+"), b -> onPlus(index))
                    .dimensions(plusX, y, 20, 20).build();

            minusButtons[i] = minus;
            plusButtons[i] = plus;
            addDrawableChild(minus);
            addDrawableChild(plus);
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Confirm"), b -> confirm())
                .dimensions(panelLeft + PANEL_WIDTH / 2 - 50, panelTop + PANEL_HEIGHT - 28, 100, 20)
                .build());

        updateButtons();
    }

    private void onPlus(int index) {
        if (remainingPoints <= 0) {
            return;
        }
        added[index]++;
        remainingPoints--;
        updateButtons();
    }

    private void onMinus(int index) {
        if (added[index] <= 0) {
            return; // can't go below what the server already has
        }
        added[index]--;
        remainingPoints++;
        updateButtons();
    }

    private void updateButtons() {
        boolean hasPoints = remainingPoints > 0;
        for (int i = 0; i < 4; i++) {
            plusButtons[i].active = hasPoints;
            minusButtons[i].active = added[i] > 0;
        }
    }

    private void confirm() {
        ClientPlayNetworking.send(new AllocateStatsPayload(added[0], added[1], added[2], added[3]));
        this.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Background dim is applied by the framework before render(); draw the panel and
        // labels first, then let super.render() paint the widgets on top.
        context.fill(panelLeft - 1, panelTop - 1, panelLeft + PANEL_WIDTH + 1, panelTop + PANEL_HEIGHT + 1, COLOR_BORDER);
        context.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, COLOR_PANEL);

        int centerX = panelLeft + PANEL_WIDTH / 2;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Stat Sheet"), centerX, panelTop + 8, COLOR_TEXT);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Champion Level " + championLevel), centerX, panelTop + 24, COLOR_TEXT);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Stat Points: " + remainingPoints), centerX, panelTop + 38, COLOR_POINTS);

        drawXpBar(context, centerX);

        for (int i = 0; i < 4; i++) {
            int value = base[i] + added[i];
            int y = rowsTop + i * ROW_HEIGHT;
            context.drawTextWithShadow(textRenderer, Text.literal(STAT_NAMES[i]), panelLeft + 16, y, COLOR_TEXT);
            context.drawTextWithShadow(textRenderer, Text.literal(String.valueOf(value)), panelLeft + 100, y, COLOR_TEXT);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    /** XP progress toward the next champion level, shown as a filled bar with "xp / needed" text. */
    private void drawXpBar(DrawContext context, int centerX) {
        int needed = ChampionLevelManager.xpForLevel(championLevel + 1);
        float fraction = needed > 0 ? Math.min(1.0f, (float) currentXP / needed) : 0.0f;

        int barLeft = panelLeft + 16;
        int barRight = panelLeft + PANEL_WIDTH - 16;
        int barTop = panelTop + 54;
        int barBottom = barTop + 11;

        context.fill(barLeft - 1, barTop - 1, barRight + 1, barBottom + 1, COLOR_BORDER);
        context.fill(barLeft, barTop, barRight, barBottom, COLOR_BAR_BG);
        int fillRight = barLeft + Math.round((barRight - barLeft) * fraction);
        context.fill(barLeft, barTop, fillRight, barBottom, COLOR_BAR_FILL);

        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(currentXP + " / " + needed + " XP"), centerX, barTop + 2, COLOR_TEXT);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // Block ESC while points are unspent.
        return remainingPoints <= 0;
    }
}
