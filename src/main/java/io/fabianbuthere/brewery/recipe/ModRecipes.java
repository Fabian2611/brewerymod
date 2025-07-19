package io.fabianbuthere.brewery.recipe;

import io.fabianbuthere.brewery.BreweryMod;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, BreweryMod.MOD_ID);

    public static final RegistryObject<RecipeSerializer<BrewingRecipe>> BREWING_SERIALIZER = SERIALIZERS.register("brewing", BrewingRecipeSerializer::new);

    public static final RecipeType<BrewingRecipe> BREWING_RECIPE_TYPE = new RecipeType<>() { public String toString() { return BreweryMod.MOD_ID + ":brewing"; } };

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }

}
