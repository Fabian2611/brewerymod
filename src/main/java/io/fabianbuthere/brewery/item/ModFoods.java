package io.fabianbuthere.brewery.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public class ModFoods {
    public static final FoodProperties COFFEE_BEAN = new FoodProperties.Builder()
            .nutrition(1)
            .saturationMod(0.2f)
            .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 10, 0), 0.5F)
            .fast()
            .build();

    public static final FoodProperties ROASTED_COFFEE_BEAN = new FoodProperties.Builder()
            .nutrition(1)
            .saturationMod(0.4f)
            .fast()
            .build();
}
