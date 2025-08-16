package io.fabianbuthere.brewery.clientdata.model;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@SuppressWarnings("removal")
public record GuideEntry(String id, String titleKey, String descriptionKey, int tint, String iconItem, int order) {
    public Component titleComponent() {
        return Component.translatable(titleKey);
    }
    public Component descriptionComponent() {
        return Component.translatable(descriptionKey);
    }
    public ItemStack potionStack() {
        ItemStack stack = new ItemStack(Items.POTION);
        stack.getOrCreateTag().putInt("CustomPotionColor", tint);
        stack.setHoverName(titleComponent());
        return stack;
    }
    public ItemStack iconStack() {
        if (iconItem == null || iconItem.isEmpty()) return potionStack();
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(iconItem));
        if (item == null || item == Items.AIR) return potionStack();
        ItemStack stack = new ItemStack(item);
        // If icon is a potion, also tint it
        if (item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) {
            stack.getOrCreateTag().putInt("CustomPotionColor", tint);
            stack.setHoverName(titleComponent());
        }
        return stack;
    }
}
