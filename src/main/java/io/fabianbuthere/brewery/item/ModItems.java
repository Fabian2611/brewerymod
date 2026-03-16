package io.fabianbuthere.brewery.item;

import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.block.ModBlocks;
import io.fabianbuthere.brewery.item.custom.GuideBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BreweryMod.MOD_ID);

    public static final RegistryObject<Item> GUIDE_BOOK = ITEMS.register("guide_book", () ->
            new GuideBookItem(new Item.Properties()));

    public static final RegistryObject<Item> COFFEE_BEAN = ITEMS.register("coffee_bean", () ->
            new ItemNameBlockItem(ModBlocks.COFFEE_CROP.get(), new Item.Properties().food(ModFoods.COFFEE_BEAN)));

    public static final RegistryObject<Item> ROASTED_COFFEE_BEAN = ITEMS.register("roasted_coffee_bean", () ->
            new Item(new Item.Properties().food(ModFoods.ROASTED_COFFEE_BEAN)));

    public static final RegistryObject<Item> GROUND_COFFEE = ITEMS.register("ground_coffee", () ->
            new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}