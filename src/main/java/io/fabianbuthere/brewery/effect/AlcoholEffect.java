package io.fabianbuthere.brewery.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/// Only instantiate using .create() to ensure correct properties.
public class AlcoholEffect extends MobEffect {
    public static final List<ItemStack> CURATIVE_ITEMS = List.of();

    /// Only for internal use.
    public AlcoholEffect() {
        super(MobEffectCategory.NEUTRAL, 0x67e33d);
    }

    /// Creates a new instance of the Alcohol effect with the specified duration and amplifier.
    /// @param duration The duration of the effect in ticks.
    /// @param amplifier The amplifier of the effect. Must be non-negative and smaller than 256.
    /// @return A new MobEffectInstance representing the Alcohol effect.
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
        if (pLivingEntity.getRandom().nextFloat() < 0.04f * (pAmplifier + 1)) {
            if (!pLivingEntity.hasEffect(MobEffects.CONFUSION)) {
                pLivingEntity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 140, pAmplifier / 2 + 1, false, false, true));
            }
        }
        if (pLivingEntity.getRandom().nextFloat() < 0.02f * (pAmplifier + 1)) {
            if (!pLivingEntity.hasEffect(MobEffects.DARKNESS)) {
                pLivingEntity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, pAmplifier / 2, false, false, true));
            }
        }
        if (pLivingEntity.getRandom().nextFloat() < 0.03f * (pAmplifier + 1)) {
            if (!pLivingEntity.hasEffect(MobEffects.BLINDNESS)) {
                pLivingEntity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, pAmplifier / 2, false, false, true));
            }
        }
        if (pLivingEntity.getRandom().nextFloat() < 0.02f * (pAmplifier + 1)) {
            if (!pLivingEntity.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                pLivingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, pAmplifier / 2, false, false, true));
            }
        }
        if (pAmplifier >= 7) {
            if (pLivingEntity.getRandom().nextFloat() < 0.05f * (pAmplifier - 6)) {
                if (!pLivingEntity.hasEffect(MobEffects.POISON)) {
                    pLivingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, 100, (pAmplifier - 4) / 2, false, false, true));
                }
            }
        }
        if (pAmplifier >= 9) {
            if (pLivingEntity.getRandom().nextFloat() < 0.04f * (pAmplifier - 8)) {
                if (!pLivingEntity.hasEffect(ModEffects.ALCOHOL_POISONING.get())) {
                    pLivingEntity.addEffect(new MobEffectInstance(ModEffects.ALCOHOL_POISONING.get(), 100, (pAmplifier - 6) / 2, false, false, true));
                }
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return pDuration % 20 == 0 || pDuration == 1;
    }
}
