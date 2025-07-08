package io.fabianbuthere.brewery.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/// Only instantiate using .create() to ensure correct properties.
public class HangoverEffect extends MobEffect {
    public static final List<ItemStack> CURATIVE_ITEMS = List.of(new ItemStack(Items.HONEY_BOTTLE));

    /// Only for internal use.
    public HangoverEffect() {
        super(MobEffectCategory.NEUTRAL, 0xeb7d34);
    }

    /// Creates a new instance of the Hangover effect with the specified duration and amplifier.
    /// @param duration The duration of the effect in ticks.
    /// @param amplifier The amplifier of the effect. Must be non-negative and smaller than 256.
    /// @return A new MobEffectInstance representing the Hangover effect.
    public static final MobEffectInstance create(int duration, int amplifier) {
        return new MobEffectInstance(ModEffects.ALCOHOL.get(), duration, amplifier, false, false, true);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return CURATIVE_ITEMS;
    }

    // Do not show effect particles
    @Override
    public void applyEffectTick(@NotNull LivingEntity pLivingEntity, int pAmplifier) {
        pLivingEntity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 3, false, false, true));

        if (pLivingEntity.getRandom().nextFloat() < 0.04f * (pAmplifier + 1)) {
            if (!pLivingEntity.hasEffect(MobEffects.BLINDNESS)) {
                pLivingEntity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 160, pAmplifier / 2, false, false, true));
            }
        }

        if (pLivingEntity.getRandom().nextFloat() < 0.02f * (pAmplifier + 1)) {
            if (!pLivingEntity.hasEffect(MobEffects.POISON)) {
                pLivingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, 220, 0, false, false, true));
            }
        }

        if (pLivingEntity.getRandom().nextFloat() < 0.04f * (pAmplifier + 1)) {
            if (!pLivingEntity.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                pLivingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, pAmplifier / 2 + 2, false, false, true));
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return pDuration % 20 == 0 || pDuration == 1;
    }
}
