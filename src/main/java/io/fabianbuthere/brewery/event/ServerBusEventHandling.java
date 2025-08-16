package io.fabianbuthere.brewery.event;

import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.config.BreweryConfig;
import io.fabianbuthere.brewery.data.BrewTypeJsonLoader;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BreweryMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerBusEventHandling {
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new BrewTypeJsonLoader());
    }
}
