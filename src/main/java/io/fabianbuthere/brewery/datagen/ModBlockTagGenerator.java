package io.fabianbuthere.brewery.datagen;

import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagGenerator extends BlockTagsProvider {
    public ModBlockTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, BreweryMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.BREWING_CAULDRON.get())
                .add(ModBlocks.DISTILLERY_STATION.get());
        tag(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.BREWING_CAULDRON.get())
                .add(ModBlocks.DISTILLERY_STATION.get());

        IntrinsicTagAppender<Block> itp = tag(BlockTags.MINEABLE_WITH_AXE);
        IntrinsicTagAppender<Block> itp2 = tag(BlockTags.NEEDS_STONE_TOOL);
        ModBlocks.FERMENTATION_BARRELS.forEach((t, b) -> {
            itp.add(b.get());
            itp2.add(b.get());
        });
    }
}
