package io.fabianbuthere.brewery.client;

import io.fabianbuthere.brewery.block.ModBlocks;
import io.fabianbuthere.brewery.block.custom.BrewingCauldronBlock;
import io.fabianbuthere.brewery.block.entity.BrewingCauldronBlockEntity;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModBlockColors {
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
            (BlockState state, BlockAndTintGetter world, BlockPos pos, int tintIndex) -> {
                if (tintIndex == 0 && world != null && pos != null) {
                    if (world.getBlockEntity(pos) instanceof BrewingCauldronBlockEntity cauldron) {
                        return cauldron.getCurrentColor();
                    }
                }
                return 0x3F76E4;
            },
            ModBlocks.BREWING_CAULDRON.get()
        );
    }
}
