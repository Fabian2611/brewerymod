package io.fabianbuthere.brewery.util;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import java.util.List;

public class ModBrewTypes {
    public static final BrewType RUM = new BrewType(
            "rum",
            3,
            5,
            List.of(
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1, false, false),
                new MobEffectInstance(MobEffects.JUMP, 600, 1, false, false)
            ),
            0xFF0000,
            "brewery.brew.rum.lore",
            "brewery.brew.rum.name"
    );

    public static final BrewType[] BREW_TYPES = new BrewType[]{RUM};
}
