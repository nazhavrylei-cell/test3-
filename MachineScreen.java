package com.example.techmod.client;

import com.example.techmod.Upgrade;
import com.example.techmod.blockentity.MachineBlockEntity;
import com.example.techmod.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Меню машини: стан, тир і кнопка прокачки. Малюється без текстур, тільки прямокутниками й текстом. */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int GREEN = 0xFF55FF55;
    private static final int RED = 0xFFFF5555;

    private Button upgradeButton;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 160;
    }

    @Override
    protected void init() {
        super.init();
        upgradeButton = Button.builder(Component.literal("Прокачати"), button -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MachineMenu.BUTTON_UPGRADE);
                    }
                })
                .bounds(leftPos + 8, topPos + 134, imageWidth - 16, 18)
                .build();
        addRenderableWidget(upgradeButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (upgradeButton != null) {
            upgradeButton.active = menu.tier() < Upgrade.MAX_TIER;
            upgradeButton.visible = menu.tier() < Upgrade.MAX_TIER;
        }
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        // рамка і фон
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1E1E1E);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF3A3A3A);

        // шкала енергії
        int barX = x + 8;
        int barY = y + 52;
        int barW = imageWidth - 16;
        graphics.fill(barX, barY, barX + barW, barY + 10, 0xFF111111);
        int capacity = Math.max(1, menu.capacity());
        int filled = Math.min(barW - 2, (barW - 2) * menu.energy() / capacity);
        graphics.fill(barX + 1, barY + 1, barX + 1 + filled, barY + 9, 0xFFE0A020);

        // шкала прогресу (для генератора та кар'єра це лише індикатор "працює")
        int progY = y + 80;
        graphics.fill(barX, progY, barX + barW, progY + 8, 0xFF111111);
        int progFilled = Math.min(barW - 2, (barW - 2) * Math.max(0, Math.min(100, menu.progress())) / 100);
        graphics.fill(barX + 1, progY + 1, barX + 1 + progFilled, progY + 7, 0xFF40C040);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int tier = menu.tier();
        graphics.drawString(font, title, 8, 8, WHITE, false);
        graphics.drawString(font, "Тир " + tier + " з " + Upgrade.MAX_TIER, 8, 22, GREEN, false);
        graphics.drawString(font,
                "Швидкість x" + Upgrade.speed(tier) + ", витрата енергії " + Upgrade.efficiencyPercent(tier) + "%",
                8, 34, GRAY, false);

        graphics.drawString(font, "Енергія: " + menu.energy() + " / " + menu.capacity(), 8, 64, WHITE, false);

        graphics.drawString(font, progressLabel(), 8, 92, WHITE, false);
        graphics.drawString(font, extraLabel(), 8, 104, GRAY, false);

        boolean linked = menu.linkedContainers() > 0;
        graphics.drawString(font,
                linked ? "Скриня під'єднана: " + menu.linkedContainers() : "Скриню не під'єднано",
                8, 116, linked ? GREEN : GRAY, false);

        if (tier < Upgrade.MAX_TIER) {
            drawCost(graphics, tier);
        } else {
            graphics.drawString(font, "Максимальний тир", 8, 138, GREEN, false);
        }
    }

    private void drawCost(GuiGraphics graphics, int tier) {
        List<Upgrade.Cost> costs = Upgrade.costFor(tier);
        // Вартість рядком над кнопкою: кожен предмет зеленим, якщо вистачає, інакше червоним
        int x = 8;
        int y = 126;
        for (Upgrade.Cost cost : costs) {
            boolean enough = minecraft != null && minecraft.player != null
                    && Upgrade.count(minecraft.player.getInventory(), cost.item()) >= cost.count();
            String text = cost.count() + "x " + cost.item().getDefaultInstance().getHoverName().getString() + "  ";
            graphics.drawString(font, text, x, y, enough ? GREEN : RED, false);
            x += font.width(text);
        }
    }

    private String progressLabel() {
        return switch (menu.kind()) {
            case MachineBlockEntity.KIND_GENERATOR -> menu.progress() > 0 ? "Горить" : "Не горить";
            case MachineBlockEntity.KIND_QUARRY -> menu.progress() > 0 ? "Робота завершена" : "Копає";
            default -> "Прогрес: " + menu.progress() + "%";
        };
    }

    private String extraLabel() {
        return switch (menu.kind()) {
            case MachineBlockEntity.KIND_GENERATOR -> "Палива: " + menu.extra();
            case MachineBlockEntity.KIND_QUARRY -> "Шар: " + menu.extra();
            default -> "У черзі: " + menu.extra();
        };
    }
}
