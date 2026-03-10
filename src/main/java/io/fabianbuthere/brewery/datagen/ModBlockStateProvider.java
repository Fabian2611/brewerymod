package io.fabianbuthere.brewery.datagen;

import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, BreweryMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        blockAndItemWithExistingModel(modLoc("cocktail_station"), ModBlocks.COCKTAIL_STATION);
    }

    private void blockAndItemWithExistingModel(ResourceLocation id, RegistryObject<Block> block) {
        ModelFile model = models().getExistingFile(id);
        simpleBlockWithItem(block.get(), model);
    }

    private void blockWithItem(RegistryObject<Block> blockRegistryObject) {
        simpleBlockWithItem(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));
    }

    public void simpleBlockItem(Block block, ModelFile model) {
        itemModels().getBuilder(ForgeRegistries.BLOCKS.getKey(block).getPath()).parent(model);
    }
}
