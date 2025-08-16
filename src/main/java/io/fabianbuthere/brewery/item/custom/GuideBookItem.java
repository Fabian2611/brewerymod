package io.fabianbuthere.brewery.item.custom;

import io.fabianbuthere.brewery.client.ClientScreens;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class GuideBookItem extends Item {
    public GuideBookItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            // Ensure the client-only class is only loaded on the client
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientScreens.openGuideBook(stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}