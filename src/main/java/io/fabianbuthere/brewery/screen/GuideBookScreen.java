package io.fabianbuthere.brewery.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.clientdata.GuideRegistry;
import io.fabianbuthere.brewery.clientdata.model.GuideEntry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("removal")
public class GuideBookScreen extends Screen {
    // Combined texture: 274x160 total.
    // Left 256x160 = book spread background, right 18x18 = slot bg.
    private static final ResourceLocation BG = new ResourceLocation(BreweryMod.MOD_ID, "textures/gui/guide_book.png");
    private static final int TEX_W = 274;
    private static final int TEX_H = 160;

    // Book spread area inside the texture
    private static final int BG_W = 256;
    private static final int BG_H = 160;

    // Slot subtexture (top-right 18x18 region)
    private static final int SLOT_U = 256;
    private static final int SLOT_V = 0;
    private static final int SLOT_W = 18;
    private static final int SLOT_H = 18;

    private static int LAST_SPREAD = 0;

    private int guiLeft, guiTop;
    private int currentSpread = 0;
    private List<GuideEntry> entries = new ArrayList<>();

    // Layout constants
    private static final int PAGE_MARGIN_X = 12;
    private static final int PAGE_MARGIN_TOP = 16;
    private static final int PAGE_INNER_GAP = 6;
    private static final int ICON_SIZE = 16;
    private static final int ICON_BG_OFFSET_X = 0;
    private static final int ICON_BG_OFFSET_Y = 0;

    private static final int TITLE_COLOR = 0x3F2A14;
    private static final int TEXT_COLOR = 0x2B2218;

    public GuideBookScreen(ItemStack stack) {
        super(Component.translatable("item.brewery.guide_book"));
    }

    @Override
    protected void init() {
        super.init();
        entries = GuideRegistry.getAllOrdered();
        currentSpread = Math.min(LAST_SPREAD, Math.max(0, (entries.size() - 1) / 2));

        guiLeft = (this.width - BG_W) / 2;
        guiTop = (this.height - BG_H) / 2;

        addButtons();
    }

    private void addButtons() {
        this.clearWidgets();
        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> prev())
                .bounds(guiLeft + 12, guiTop + BG_H - 22, 20, 18)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> next())
                .bounds(guiLeft + BG_W - 32, guiTop + BG_H - 22, 20, 18)
                .build());
    }

    private void prev() {
        if (currentSpread > 0) {
            currentSpread--;
            LAST_SPREAD = currentSpread;
        }
    }

    private void next() {
        int maxSpread = Math.max(0, (entries.size() - 1) / 2);
        if (currentSpread < maxSpread) {
            currentSpread++;
            LAST_SPREAD = currentSpread;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Left/Right arrow (GLFW 263/262)
        if (keyCode == 263) { prev(); return true; }
        if (keyCode == 262) { next(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta < 0) next(); else if (delta > 0) prev();
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, BG);

        // Draw the 256x160 spread from the combined 274x160 texture
        g.blit(BG, guiLeft, guiTop, 0, 0, BG_W, BG_H, TEX_W, TEX_H);

        // Page geometry
        int pageWidth = (BG_W / 2) - PAGE_MARGIN_X * 2;
        int leftPageX = guiLeft + PAGE_MARGIN_X;
        int rightPageX = guiLeft + (BG_W / 2) + PAGE_MARGIN_X;
        int contentTop = guiTop + PAGE_MARGIN_TOP;

        // Indices
        int leftIndex = currentSpread * 2;
        int rightIndex = leftIndex + 1;

        renderPage(g, leftIndex, leftPageX, contentTop, pageWidth);
        renderPage(g, rightIndex, rightPageX, contentTop, pageWidth);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderPage(GuiGraphics g, int index, int x, int top, int pageWidth) {
        if (index < 0 || index >= entries.size()) return;

        GuideEntry e = entries.get(index);
        Font font = this.font;
        int y = top;

        // Title centered
        Component title = e.titleComponent();
        int titleWidth = font.width(title);
        int titleX = x + Math.max(0, (pageWidth - titleWidth) / 2);
        g.drawString(font, title, titleX, y, TITLE_COLOR, false);
        y += font.lineHeight + PAGE_INNER_GAP;

        // Icon centered, with 18x18 slot background behind it at (-1, -1) offset
        ItemStack icon = e.iconStack();
        int iconX = x + Math.max(0, (pageWidth - ICON_SIZE) / 2);

        // Blit the slot background using the subregion from the same texture
        int bgX = iconX + ICON_BG_OFFSET_X;
        int bgY = y + ICON_BG_OFFSET_Y;
        g.blit(BG, bgX, bgY, SLOT_U, SLOT_V, SLOT_W, SLOT_H, TEX_W, TEX_H);

        // Then render the item on top
        g.renderItem(icon, iconX, y);
        y += ICON_SIZE + PAGE_INNER_GAP + 2;

        // Description: vanilla book-style wrap
        List<FormattedCharSequence> lines = font.split(e.descriptionComponent(), pageWidth);
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, x, y, TEXT_COLOR, false);
            y += font.lineHeight;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
