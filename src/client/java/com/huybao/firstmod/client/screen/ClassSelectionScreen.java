package com.huybao.firstmod.client.screen;

import com.huybao.firstmod.data.PlayerClass;
import com.huybao.firstmod.network.SelectClassC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

// First-join class picker. One button per class; its stat changes show in a
// tooltip when you hover the button, to keep the panel small and uncluttered.
// Can't be dismissed (no ESC, no close button) — the only way out is choosing a class.
public class ClassSelectionScreen extends Screen {

    private static final int PANEL_WIDTH = 200;
    private static final int ROW_HEIGHT = 26;
    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROWS_TOP_OFFSET = 44;

    private static final int COLOR_PANEL = 0xE0101018;
    private static final int COLOR_BORDER = 0xFF3F3F8F;
    private static final int COLOR_TITLE = 0xFFFFD700;
    private static final int COLOR_HINT = 0xFF8080A0;

    // Display name + the stat lines shown in the hover tooltip.
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
                    new String[]{"No stat changes", "+20% XP gain"}),
    };

    private int panelLeft;
    private int panelTop;
    private int panelHeight;
    private int rowsTop;

    public ClassSelectionScreen() {
        super(Text.literal("Choose Your Class"));
    }

    @Override
    protected void init() {
        panelHeight = ROWS_TOP_OFFSET + OPTIONS.length * ROW_HEIGHT + 12;
        panelLeft = (this.width - PANEL_WIDTH) / 2;
        panelTop = (this.height - panelHeight) / 2;
        rowsTop = panelTop + ROWS_TOP_OFFSET;

        int buttonX = panelLeft + (PANEL_WIDTH - BUTTON_WIDTH) / 2;
        for (int i = 0; i < OPTIONS.length; i++) {
            ClassOption option = OPTIONS[i];
            int y = rowsTop + i * ROW_HEIGHT;
            ButtonWidget button = ButtonWidget.builder(Text.literal(option.label()), b -> select(option.playerClass()))
                    .dimensions(buttonX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .tooltip(Tooltip.of(Text.literal(String.join("\n", option.description()))))
                    .build();
            addDrawableChild(button);
        }
    }

    private void select(PlayerClass playerClass) {
        ClientPlayNetworking.send(new SelectClassC2SPacket(playerClass));
        this.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(panelLeft - 1, panelTop - 1, panelLeft + PANEL_WIDTH + 1, panelTop + panelHeight + 1, COLOR_BORDER);
        context.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + panelHeight, COLOR_PANEL);

        int centerX = panelLeft + PANEL_WIDTH / 2;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Choose Your Class"), centerX, panelTop + 14, COLOR_TITLE);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Hover a class to see its stats"), centerX, panelTop + 28, COLOR_HINT);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // must pick a class
    }
}
