package io.fabianbuthere.brewery.util;

import net.minecraft.world.item.Item;

public record ItemStackInput(Item item, int minCount, int maxCount) {
    public int optimalCount() {
        return (int)Math.floor((double)(minCount + maxCount) / 2);
    }
}