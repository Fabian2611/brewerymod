package io.fabianbuthere.brewery.event;

import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.config.BreweryConfig;
import io.fabianbuthere.brewery.effect.ModEffects;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.MobEffectEvent;

@Mod.EventBusSubscriber(modid = BreweryMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StackingEffectHandler {
    public static final ThreadLocal<Boolean> IGNORE = ThreadLocal.withInitial(() -> false);

    private static boolean isStackingEffect(MobEffect effect) {
        return effect == ModEffects.ALCOHOL.get();
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (IGNORE.get()) return; // Let our custom application go through unmodified

        LivingEntity entity = event.getEntity();
        MobEffectInstance incoming = event.getEffectInstance();
        MobEffect effect = incoming.getEffect();

        if (entity.level().isClientSide) return;
        if (!isStackingEffect(effect)) return;

        MobEffectInstance existing = entity.getEffect(effect);
        if (existing == null) return;

        event.setResult(Event.Result.DENY);
        int existingLevel = existing.getAmplifier() + 1;
        int incomingLevel = incoming.getAmplifier() + 1;
        int newLevel = existingLevel + incomingLevel;
        int newAmplifier = Math.max(0, Math.min(newLevel - 1, BreweryConfig.MAX_ALCOHOL_STACKED_AMPLIFIER.get()));

        int newDuration = Math.min(safeAdd(existing.getDuration(), Mth.floor(incoming.getDuration() * 0.5f)), BreweryConfig.MAX_ALCOHOL_STACKED_DURATION_SECONDS.get() * 20);

        boolean ambient = existing.isAmbient();
        boolean visible = existing.isVisible();
        boolean showIcon = existing.showIcon();

        MobEffectInstance stacked = new MobEffectInstance(
                effect,
                newDuration,
                newAmplifier,
                ambient,
                visible,
                showIcon
        );

        IGNORE.set(true);
        try {
            entity.removeEffect(effect);
            entity.addEffect(stacked);
        } finally {
            IGNORE.set(false);
        }
    }

    private static int safeAdd(int a, int b) {
        long sum = (long) a + (long) b;
        return (int) Math.min(sum, Integer.MAX_VALUE);
    }
}
