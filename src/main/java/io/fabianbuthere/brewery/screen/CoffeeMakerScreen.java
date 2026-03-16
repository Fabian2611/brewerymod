package io.fabianbuthere.brewery.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.fabianbuthere.brewery.BreweryMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

@SuppressWarnings("removal")
public class CoffeeMakerScreen extends AbstractContainerScreen<CoffeeMakerMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BreweryMod.MOD_ID, "textures/gui/coffee_maker.png");

    private static final int LEFT_ARROW_X = 34;
    private static final int LEFT_ARROW_Y = 44;
    private static final int LEFT_ARROW_W = 18;
    private static final int LEFT_ARROW_H = 9;
    private static final int LEFT_ARROW_U = 194;
    private static final int LEFT_ARROW_V = 0;

    private static final int LEFT_LED_X = 53;
    private static final int LEFT_LED_Y = 45;
    private static final int LEFT_LED_W = 7;
    private static final int LEFT_LED_H = 7;
    private static final int LEFT_LED_U = 212;
    private static final int LEFT_LED_V = 0;

    private static final int RIGHT_ARROW_X = 134;
    private static final int RIGHT_ARROW_Y = 26;
    private static final int RIGHT_ARROW_W = 17;
    private static final int RIGHT_ARROW_H = 25;
    private static final int RIGHT_ARROW_U = 176;
    private static final int RIGHT_ARROW_V = 0;

    public CoffeeMakerScreen(CoffeeMakerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        g.blit(TEXTURE, x, y, 0, 0, 176, 166);

        // Left (vertical fill, top -> bottom)
        if (menu.isLeftCrafting()) {
            int h = menu.getLeftScaledProgress(LEFT_ARROW_H);
            g.blit(TEXTURE,
                    x + LEFT_ARROW_X, y + LEFT_ARROW_Y,
                    LEFT_ARROW_U, LEFT_ARROW_V,
                    LEFT_ARROW_W, h);

            g.blit(TEXTURE,
                    x + LEFT_LED_X, y + LEFT_LED_Y,
                    LEFT_LED_U, LEFT_LED_V,
                    LEFT_LED_W, LEFT_LED_H);
        }

        // Right (vertical fill, top -> bottom)
        if (menu.isRightCrafting()) {
            int h = menu.getRightScaledProgress(RIGHT_ARROW_H);
            g.blit(TEXTURE,
                    x + RIGHT_ARROW_X, y + RIGHT_ARROW_Y,
                    RIGHT_ARROW_U, RIGHT_ARROW_V,
                    RIGHT_ARROW_W, h);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }
}
