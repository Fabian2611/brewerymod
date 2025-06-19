package io.fabianbuthere.brewery.datagen;

import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
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

    }

    @SuppressWarnings("removal")
    private ItemModelBuilder simpleItem(RegistryObject<Item> item) {
        return withExistingParent(item.getId().getPath(),
                new ResourceLocation("item/generated")).texture("layer0",
                new ResourceLocation(BreweryMod.MOD_ID, "item/" + item.getId().getPath()));
    }
}
