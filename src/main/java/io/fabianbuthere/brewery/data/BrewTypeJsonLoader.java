package io.fabianbuthere.brewery.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import io.fabianbuthere.brewery.config.BreweryConfig;
import io.fabianbuthere.brewery.util.BrewType;
import io.fabianbuthere.brewery.util.BrewTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("removal")
public class BrewTypeJsonLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String DIRECTORY = "brew_types";

    public BrewTypeJsonLoader() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, net.minecraft.util.profiling.ProfilerFiller profiler) {
        BrewTypeRegistry.clear();
        boolean useBuiltinBrews = BreweryConfig.ENABLE_BUILTIN_BREWS.get();
        object.forEach((id, jsonElement) -> {
            try {
                JsonObject json = GsonHelper.convertToJsonObject(jsonElement, "brew_type");

                if (!useBuiltinBrews && GsonHelper.getAsBoolean(json, "builtin", false)) {
                    LOGGER.info("Skipping builtin BrewType {} as builtin brews are disabled.", id);
                    return;
                }

                int maxAlcoholLevel = GsonHelper.getAsInt(json, "maxAlcoholLevel");
                int maxPurity = GsonHelper.getAsInt(json, "maxPurity");
                int tintColor = GsonHelper.getAsInt(json, "tintColor");
                String customLore = GsonHelper.getAsString(json, "customLore");
                String customName = GsonHelper.getAsString(json, "customName");
                List<MobEffectInstance> effects = new ArrayList<>();
                JsonArray effectsArray = GsonHelper.getAsJsonArray(json, "effects");
                for (JsonElement effElem : effectsArray) {
                    JsonObject effObj = effElem.getAsJsonObject();
                    String effectId = GsonHelper.getAsString(effObj, "effect");
                    int duration = GsonHelper.getAsInt(effObj, "duration");
                    int amplifier = GsonHelper.getAsInt(effObj, "amplifier");
                    // Accept both registry names (e.g. minecraft:speed) and translation keys (e.g. effect.minecraft.speed)
                    MobEffect mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(effectId));
                    if (mobEffect == null) {
                        // Try to resolve from translation key (e.g. effect.minecraft.speed)
                        String[] split = effectId.split("\\.");
                        if (split.length > 1) {
                            mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(split[split.length-2], split[split.length-1]));
                        }
                    }
                    if (mobEffect == null) mobEffect = MobEffects.LUCK; // fallback
                    // Always set showParticles to false for all loaded effects
                    effects.add(new MobEffectInstance(mobEffect, duration, amplifier, false, false));
                }
                BrewType brewType = new BrewType(
                        id.getPath(),
                        maxAlcoholLevel,
                        maxPurity,
                        effects,
                        tintColor,
                        customLore,
                        customName
                );
                BrewTypeRegistry.register(brewType);
            } catch (Exception e) {
                LOGGER.error("Failed to load BrewType {}: {}", id, e);
            }
        });
        LOGGER.info("Loaded {} BrewTypes from JSON. Registered BrewTypes: {}", BrewTypeRegistry.getAll().size(), BrewTypeRegistry.getAll().keySet());
    }
}
