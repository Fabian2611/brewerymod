package io.fabianbuthere.brewery.screen;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class FilterSlot extends SlotItemHandler {
    private final Item[] allowedItems;

    public FilterSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition, Item[] allowedItems) {
        super(itemHandler, index, xPosition, yPosition);
        this.allowedItems = allowedItems;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        for (Item item : allowedItems) {
            if (stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }
}
