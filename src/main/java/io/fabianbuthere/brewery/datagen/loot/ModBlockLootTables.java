package io.fabianbuthere.brewery.datagen.loot;

import io.fabianbuthere.brewery.block.ModBlocks;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

public class ModBlockLootTables extends BlockLootSubProvider {
    public ModBlockLootTables() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        this.dropSelf(ModBlocks.BREWING_CAULDRON.get());
        this.dropSelf(ModBlocks.DISTILLERY_STATION.get());
        for (var barrel : ModBlocks.FERMENTATION_BARRELS.values()) {
            this.dropSelf(barrel.get());
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get)::iterator;
    }
}
