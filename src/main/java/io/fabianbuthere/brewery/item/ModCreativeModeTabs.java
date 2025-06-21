package io.fabianbuthere.brewery.item;

import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BreweryMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> BREWERY_TAB = CREATIVE_MODE_TABS.register("brewery_tab", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(Items.POTION))
            .title(Component.translatable("creativetab.brewery_tab"))
            .displayItems((pParameters, pOutput) -> {
                pOutput.accept(ModBlocks.BREWING_CAULDRON.get());
                pOutput.accept(ModBlocks.DISTILLERY_STATION.get());
                for (var barrel : ModBlocks.FERMENTATION_BARRELS.values()) {
                    pOutput.accept(barrel.get());
                }
            })
            .build());


    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
