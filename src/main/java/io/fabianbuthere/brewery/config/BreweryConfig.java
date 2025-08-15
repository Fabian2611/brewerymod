package io.fabianbuthere.brewery.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class BreweryConfig {
    public static final ForgeConfigSpec COMMON_CONFIG;
    public static final ForgeConfigSpec SERVER_CONFIG;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BUILTIN_BREWS;
    public static final ForgeConfigSpec.IntValue MAX_ALCOHOL_STACKED_DURATION_SECONDS;
    public static final ForgeConfigSpec.IntValue MAX_ALCOHOL_STACKED_AMPLIFIER;

    static {
        // --- COMMON --- //

        ForgeConfigSpec.Builder commonBuilder = new ForgeConfigSpec.Builder();

        commonBuilder.comment("General Brewery Settings").push("general");
        ENABLE_BUILTIN_BREWS = commonBuilder
                .comment("Enable builtin brew types. Marked for replacement in the future.")
                .define("enableBuiltinBrews", true);
        commonBuilder.pop();

        COMMON_CONFIG = commonBuilder.build();


        // --- SERVER --- //

        ForgeConfigSpec.Builder serverBuilder = new ForgeConfigSpec.Builder();

        serverBuilder.comment("Alcohol Stacking Settings").push("alcohol_stacking");
        MAX_ALCOHOL_STACKED_DURATION_SECONDS = serverBuilder
                .comment("Maximum duration for stacked alcohol effects in seconds")
                .defineInRange("maxAlcoholStackedDurationSeconds", 1200, 1, Integer.MAX_VALUE);
        MAX_ALCOHOL_STACKED_AMPLIFIER = serverBuilder
                .comment("Maximum amplifier for stacked alcohol effects")
                .defineInRange("maxAlcoholStackedAmplifier", 9, 0, 255);
        serverBuilder.pop();

        SERVER_CONFIG = serverBuilder.build();
    }
}
