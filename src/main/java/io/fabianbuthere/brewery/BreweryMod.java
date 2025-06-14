package io.fabianbuthere.brewery;

import com.mojang.logging.LogUtils;
import io.fabianbuthere.brewery.block.ModBlocks;
import io.fabianbuthere.brewery.block.entity.ModBlockEntities;
import io.fabianbuthere.brewery.item.ModCreativeModeTabs;
import io.fabianbuthere.brewery.item.ModItems;
import io.fabianbuthere.brewery.recipe.ModRecipes;
import io.fabianbuthere.brewery.screen.DistilleryStationScreen;
import io.fabianbuthere.brewery.screen.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(BreweryMod.MOD_ID)
public class BreweryMod
{
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "brewery";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public BreweryMod(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        ModCreativeModeTabs.register(modEventBus);

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);

        ModBlockEntities.register(modEventBus);

        ModRecipes.register(modEventBus);
        ModMenus.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {

    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.RUBY);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {

    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event)
        {
            MenuScreens.register(ModMenus.DISTILLERY_STATION_MENU.get(), DistilleryStationScreen::new);
        }
    }

}
