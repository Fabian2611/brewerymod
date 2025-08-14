package io.fabianbuthere.brewery.block;

import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.block.custom.*;
import io.fabianbuthere.brewery.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BreweryMod.MOD_ID);

    public static final RegistryObject<Block> BREWING_CAULDRON = registerBlock("brewing_cauldron", () ->
            new BrewingCauldronBlock(BlockBehaviour.Properties.copy(Blocks.CAULDRON)));
    public static final RegistryObject<Block> DISTILLERY_STATION = registerBlock("distillery_station", () ->
            new DistilleryStationBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    public static final EnumMap<WoodType, RegistryObject<Block>> FERMENTATION_BARRELS = new EnumMap<>(WoodType.class);

    public static final RegistryObject<Block> BREW_SHELF = registerBlock("brew_shelf", () ->
            new BrewShelfBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).noOcclusion()));

    static {
        for (WoodType type : WoodType.values()) {
            String name = "fermentation_barrel_" + type.getSerializedName();
            RegistryObject<Block> barrel = registerBlock(name, () -> new FermentationBarrelBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS), type));
            FERMENTATION_BARRELS.put(type, barrel);
        }
    }

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        // Use custom BlockItem for fermentation barrels
        for (WoodType type : WoodType.values()) {
            String barrelName = "fermentation_barrel_" + type.getSerializedName();
            if (name.equals(barrelName)) {
                return ModItems.ITEMS.register(name, () -> new FermentationBarrelBlockItem(block.get(), new Item.Properties(), type));
            }
        }
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
