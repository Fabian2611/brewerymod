package io.fabianbuthere.brewery.util;

import io.fabianbuthere.brewery.BreweryMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

@SuppressWarnings("removal")
public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> METAL_DETECTOR_BLOCKS = tag("metal_detector_blocks");

        private static TagKey<Block> tag(String name) {
            return BlockTags.create(new ResourceLocation(BreweryMod.MOD_ID, name));
        }
    }
    public static class Items {
        private static TagKey<Item> tag(String name) {
            return ItemTags.create(new ResourceLocation(BreweryMod.MOD_ID, name));
        }
    }
}
