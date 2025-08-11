package io.fabianbuthere.brewery.client;

import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.block.entity.ModBlockEntities;
import io.fabianbuthere.brewery.client.render.BrewShelfRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BreweryMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBlockEntityRenderers {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.BREW_SHELF.get(), BrewShelfRenderer::new);
    }
}
