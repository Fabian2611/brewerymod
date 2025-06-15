package io.fabianbuthere.brewery.block.entity;

import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BreweryMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<BrewingCauldronBlockEntity>> BREWING_CAULDRON = BLOCK_ENTITIES.register(
            "brewing_cauldron",
            () -> BlockEntityType.Builder.of(BrewingCauldronBlockEntity::new, ModBlocks.BREWING_CAULDRON.get()).build(null)
    );

    public static final RegistryObject<BlockEntityType<DistilleryStationBlockEntity>> DISTILLERY_STATION = BLOCK_ENTITIES.register(
            "distillery_station",
            () -> BlockEntityType.Builder.of(DistilleryStationBlockEntity::new, ModBlocks.DISTILLERY_STATION.get()).build(null)
    );

    public static final RegistryObject<BlockEntityType<FermentationBarrelBlockEntity>> FERMENTATION_BARREL = BLOCK_ENTITIES.register(
            "fermentation_barrel",
            () -> BlockEntityType.Builder.of(FermentationBarrelBlockEntity::new, ModBlocks.FERMENTATION_BARREL.get()).build(null)
    );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
