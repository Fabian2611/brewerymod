package io.fabianbuthere.brewery.datagen;

import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.block.custom.WoodType;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, BreweryMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (WoodType type : WoodType.values()) {
            String name = "fermentation_barrel_" + type.getSerializedName();
            withExistingParent(name, modLoc("block/" + type.getSerializedName()) + "_fermentation_barrel")
                .transforms()
                .transform(ItemDisplayContext.GUI)
                    .rotation(30, 225, 0)
                    .scale(0.65F)
                    .translation(0, 0, 0)
                    .end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                    .scale(0.5F)
                    .end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                    .scale(0.5F)
                    .end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                    .scale(0.5F)
                    .end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                    .scale(0.5F)
                    .end()
                .transform(ItemDisplayContext.GROUND)
                    .scale(0.7F)
                    .end()
                .end();
        }
        withExistingParent("brewing_cauldron", modLoc("block/brewing_cauldron_level_0"))
            .transforms()
            .transform(ItemDisplayContext.GUI)
                .rotation(30, 225, 0)
                .scale(0.65F)
                .translation(0, 0, 0)
                .end()
            .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                .scale(0.5F)
                .end()
            .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                .scale(0.5F)
                .end()
            .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .scale(0.5F)
                .end()
            .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                .scale(0.5F)
                .end()
            .transform(ItemDisplayContext.GROUND)
                .scale(0.7F)
                .end()
            .end();
    }

    @SuppressWarnings("removal")
    private ItemModelBuilder simpleItem(RegistryObject<Item> item) {
        return withExistingParent(item.getId().getPath(),
                new ResourceLocation("item/generated")).texture("layer0",
                new ResourceLocation(BreweryMod.MOD_ID, "item/" + item.getId().getPath()));
    }
}
