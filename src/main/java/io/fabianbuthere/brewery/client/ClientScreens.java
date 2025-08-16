package io.fabianbuthere.brewery.client;

import io.fabianbuthere.brewery.screen.GuideBookScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public final class ClientScreens {
    private ClientScreens() {}

    public static void openGuideBook(ItemStack stack) {
        Minecraft.getInstance().setScreen(new GuideBookScreen(stack));
    }
}