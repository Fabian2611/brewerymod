package io.fabianbuthere.brewery.block.custom;

import io.fabianbuthere.brewery.item.ModCreativeModeTabs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class FermentationBarrelBlockItem extends BlockItem {
    private final WoodType woodType;

    public FermentationBarrelBlockItem(Block block, Properties properties, WoodType woodType) {
        super(block, properties);
        this.woodType = woodType;
    }

    @Override
    public void onCraftedBy(ItemStack stack, net.minecraft.world.level.Level world, net.minecraft.world.entity.player.Player player) {
        super.onCraftedBy(stack, world, player);
        setWoodTypeTag(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        setWoodTypeTag(stack);
        return super.isFoil(stack);
    }

    private void setWoodTypeTag(ItemStack stack) {
        stack.getOrCreateTag().putString("wood_type", woodType.getSerializedName());
    }

    private CreativeModeTab getCreativeTab() {
        return ModCreativeModeTabs.BREWERY_TAB.get();
    }
}
