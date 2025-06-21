package io.fabianbuthere.brewery.datagen;

import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.block.ModBlocks;
import io.fabianbuthere.brewery.block.custom.BrewingCauldronBlock;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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
        cauldronLikeBlockWithItem(ModBlocks.BREWING_CAULDRON.get(), "water_cauldron_level_");
        blockWithItem(ModBlocks.DISTILLERY_STATION);
        fermentationBarrels();
    }

    private void blockWithItem(RegistryObject<Block> blockRegistryObject) {
        simpleBlockWithItem(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));
    }

    public void cauldronLikeBlockWithItem(Block block, String liquidTexturePrefix) {
        float[] waterHeights = {0f, 9.0f, 12.0f, 15.0f};
        for (int level = 0; level <= 3; level++) {
            String baseName = ForgeRegistries.BLOCKS.getKey(block).getPath() + "_level_" + level;
            String liquidName = baseName + "_liquid";
            models().withExistingParent(baseName, mcLoc("block/cauldron"))
                    .texture("top", mcLoc("block/cauldron_top"))
                    .texture("bottom", mcLoc("block/cauldron_bottom"))
                    .texture("side", mcLoc("block/cauldron_side"))
                    .texture("inner", mcLoc("block/cauldron_inner"));
            if (level > 0) {
                float y = waterHeights[level];
                models().getBuilder(liquidName)
                        .texture("liquid", mcLoc("block/water_still"))
                        .element()
                        .from(2, y, 2)
                        .to(14, y + 0.1f, 14)
                        .face(Direction.UP)
                        .texture("#liquid")
                        .tintindex(0)
                        .end()
                        .end();
            }
            getMultipartBuilder(block)
                    .part()
                    .modelFile(models().getExistingFile(modLoc("block/" + baseName)))
                    .addModel()
                    .condition(BrewingCauldronBlock.BREW_LEVEL, level)
                    .end();
            if (level > 0) {
                getMultipartBuilder(block)
                        .part()
                        .modelFile(models().getExistingFile(modLoc("block/" + liquidName)))
                        .addModel()
                        .condition(BrewingCauldronBlock.BREW_LEVEL, level)
                        .end();
            }
        }
        // Item model — just use vanilla cauldron item texture and model
        itemModels().withExistingParent(
                ForgeRegistries.BLOCKS.getKey(block).getPath(),
                mcLoc("item/cauldron")
        );
    }

    // Add this helper for correct item model generation
    public void simpleBlockItem(Block block, ModelFile model) {
        itemModels().getBuilder(ForgeRegistries.BLOCKS.getKey(block).getPath()).parent(model);
    }

    private void fermentationBarrels() {
        for (var entry : ModBlocks.FERMENTATION_BARRELS.entrySet()) {
            var type = entry.getKey();
            var block = entry.getValue().get();
            String wood = type.getSerializedName();
            boolean isSpruce = wood.equals("spruce");
            var top = isSpruce ? mcLoc("block/barrel_top") : modLoc("block/" + wood + "_barrel_top");
            var topOpen = isSpruce ? mcLoc("block/barrel_top_open") : modLoc("block/" + wood + "_barrel_top_open");
            var bottom = isSpruce ? mcLoc("block/barrel_bottom") : modLoc("block/" + wood + "_barrel_bottom");
            var side = isSpruce ? mcLoc("block/barrel_side") : modLoc("block/" + wood + "_barrel_side");
            // Closed model
            ModelFile closed = models().cube(
                wood + "_fermentation_barrel",
                bottom,
                top,
                side,
                side,
                side,
                side
            );
            // Opened model (custom top)
            ModelFile open = models().cube(
                wood + "_fermentation_barrel_open",
                bottom,
                topOpen,
                side,
                side,
                side,
                side
            );
            getVariantBuilder(block)
                .partialState().with(BlockStateProperties.OPEN, false)
                .modelForState().modelFile(closed).addModel()
                .partialState().with(BlockStateProperties.OPEN, true)
                .modelForState().modelFile(open).addModel();
            // Item model (fix: use the block model as parent)
            simpleBlockItem(block, closed);
        }
    }
}
