package com.huybao.firstmod.client.screen;

import com.huybao.firstmod.data.PlayerClass;
import com.huybao.firstmod.network.SelectClassC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

// First-join class picker. One button per class with its stat changes listed beside it.
// Can't be dismissed (no ESC, no close button) — the only way out is choosing a class.
public class ClassSelectionScreen extends Screen {

    private static final int PANEL_WIDTH = 410;
    private static final int PANEL_HEIGHT = 300;
    private static final int ROW_HEIGHT = 62;
    private static final int LINE_HEIGHT = 10;
    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_HEIGHT = 20;

    private static final int COLOR_PANEL = 0xE0101018;
    private static final int COLOR_BORDER = 0xFF3F3F8F;
    private static final int COLOR_TITLE = 0xFFFFD700;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_DESC = 0xFFB0B0C0;

    // Display name + the description lines shown next to each button.
    private record ClassOption(PlayerClass playerClass, String label, String[] description) {
    }

    private static final ClassOption[] OPTIONS = {
            new ClassOption(PlayerClass.KNIGHT, "Knight",
                    new String[]{"+2 attack damage", "+2 max hearts", "+3 armor", "+3 armor toughness"}),
            new ClassOption(PlayerClass.ARCHER, "Archer",
                    new String[]{"+30% movement speed", "+4 projectile damage", "-2 max hearts"}),
            new ClassOption(PlayerClass.GIANT, "Giant",
                    new String[]{"+8 max hearts", "+3 attack damage", "+5 armor",
                            "+20% knockback resist", "-20% movement speed", "1.5x size"}),
            new ClassOption(PlayerClass.VAGABOND, "No Class",
                    new String[]{"No stat changes", "-10% XP gain"}),
    };

    private int panelLeft;
    private int panelTop;
    private int rowsTop;

    public ClassSelectionScreen() {
        super(Text.literal("Choose Your Class"));
    }

    @Override
    protected void init() {
        panelLeft = (this.width - PANEL_WIDTH) / 2;
        panelTop = (this.height - PANEL_HEIGHT) / 2;
        rowsTop = panelTop + 40;

        for (int i = 0; i < OPTIONS.length; i++) {
            ClassOption option = OPTIONS[i];
            int y = rowsTop + i * ROW_HEIGHT;
            addDrawableChild(ButtonWidget.builder(Text.literal(option.label()), b -> select(option.playerClass()))
                    .dimensions(panelLeft + 16, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }
    }

    private void select(PlayerClass playerClass) {
        ClientPlayNetworking.send(new SelectClassC2SPacket(playerClass));
        this.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(panelLeft - 1, panelTop - 1, panelLeft + PANEL_WIDTH + 1, panelTop + PANEL_HEIGHT + 1, COLOR_BORDER);
        context.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, COLOR_PANEL);

        int centerX = panelLeft + PANEL_WIDTH / 2;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Choose Your Class"), centerX, panelTop + 14, COLOR_TITLE);

        int descX = panelLeft + 16 + BUTTON_WIDTH + 16;
        for (int i = 0; i < OPTIONS.length; i++) {
            String[] lines = OPTIONS[i].description();
            int rowY = rowsTop + i * ROW_HEIGHT;
            for (int line = 0; line < lines.length; line++) {
                int color = line == 0 ? COLOR_TEXT : COLOR_DESC;
                context.drawTextWithShadow(textRenderer, Text.literal(lines[line]), descX, rowY + line * LINE_HEIGHT, color);
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // must pick a class
    }
}
