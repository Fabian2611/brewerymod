package io.fabianbuthere.brewery.effect;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/// Only instantiate using .create() to ensure correct properties.
public class AlcoholPoisoningEffect extends MobEffect {
    public static final List<ItemStack> CURATIVE_ITEMS = List.of(new ItemStack(Items.HONEY_BOTTLE));

    /// Only for internal use.
    public AlcoholPoisoningEffect() {
        super(MobEffectCategory.HARMFUL, 0xba630b);
    }

    /// Creates a new instance of the Alcohol effect with the specified duration and amplifier.
    /// @param duration The duration of the effect in ticks.
    /// @param amplifier The amplifier of the effect. Must be non-negative and smaller than 256.
    /// @return A new MobEffectInstance representing the Alcohol effect.
    public static MobEffectInstance create(int duration, int amplifier) {
        return new MobEffectInstance(ModEffects.ALCOHOL_POISONING.get(), duration, amplifier, false, true, true);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return CURATIVE_ITEMS;
    }

    @Override
    public void applyEffectTick(@NotNull LivingEntity pLivingEntity, int pAmplifier) {
        if (pLivingEntity.level().isClientSide) return;
        pLivingEntity.hurt(pLivingEntity.damageSources().magic(), 0.2f * (pAmplifier + 1));
        if (pLivingEntity instanceof ServerPlayer player && player.getFoodData().getFoodLevel() > 1) {
            player.getFoodData().setFoodLevel(Math.max(0, player.getFoodData().getFoodLevel() - 1));
        }
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return pDuration % 20 == 0 || pDuration == 1;
    }
}
