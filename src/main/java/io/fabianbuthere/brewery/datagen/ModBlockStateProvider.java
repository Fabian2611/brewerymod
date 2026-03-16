package io.fabianbuthere.brewery.datagen;

import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.block.ModBlocks;
import io.fabianbuthere.brewery.block.custom.CoffeeCropBlock;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
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

        makeCoffeeCrop((CropBlock) ModBlocks.COFFEE_CROP.get(), "coffee_stage", "coffee_stage");
    }

    public void makeCoffeeCrop(CropBlock block, String modelName, String textureName) {
        getVariantBuilder(block).forAllStates(state -> coffeeStates(state, block, modelName, textureName));
    }

    private ConfiguredModel[] coffeeStates(BlockState state, CropBlock block, String modelName, String textureName) {
        ConfiguredModel[] models = new ConfiguredModel[1];
        models[0] = new ConfiguredModel(models().crop(modelName + state.getValue(((CoffeeCropBlock)block).getAgeProperty()),
                ResourceLocation.fromNamespaceAndPath(BreweryMod.MOD_ID, "block/" + textureName + state.getValue(((CoffeeCropBlock)block).getAgeProperty()))).renderType("cutout"));

        return models;
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
