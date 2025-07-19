package io.fabianbuthere.brewery.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class BreweryConfig {
    public static final ForgeConfigSpec COMMON_CONFIG;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BUILTIN_BREWS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("General Brewery Settings").push("general");
        ENABLE_BUILTIN_BREWS = builder
                .comment("Enable builtin brew types")
                .define("enableBuiltinBrews", true);
        builder.pop();

        COMMON_CONFIG = builder.build();
    }
}