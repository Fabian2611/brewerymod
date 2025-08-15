package io.fabianbuthere.brewery.effect;

import io.fabianbuthere.brewery.BreweryMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, BreweryMod.MOD_ID);

    public static final RegistryObject<MobEffect> ALCOHOL = EFFECTS.register("alcohol", AlcoholEffect::new);
    public static final RegistryObject<MobEffect> HANGOVER = EFFECTS.register("hangover", HangoverEffect::new);
    public static final RegistryObject<MobEffect> ALCOHOL_POISONING = EFFECTS.register("alcohol_poisoning", AlcoholPoisoningEffect::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
