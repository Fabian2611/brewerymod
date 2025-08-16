package io.fabianbuthere.brewery.event;

import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.clientdata.GuideEntryJsonLoader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BreweryMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientBusEventHandling {
    @SubscribeEvent
    public static void onClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new GuideEntryJsonLoader());
    }
}
