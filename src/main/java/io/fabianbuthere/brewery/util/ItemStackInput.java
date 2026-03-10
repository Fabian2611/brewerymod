package io.fabianbuthere.brewery.util;

import net.minecraft.world.item.Item;

public record ItemStackInput(Item item, int minCount, int maxCount) { }