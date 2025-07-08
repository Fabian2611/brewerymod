package io.fabianbuthere.brewery.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.util.BrewType;
import io.fabianbuthere.brewery.util.ModBrewTypes;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("removal")
public class BrewTypeProvider implements DataProvider {
    private final PackOutput output;

    public BrewTypeProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public @NotNull CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (BrewType brewType : ModBrewTypes.BREW_TYPES) {
            JsonObject json = toJson(brewType);
            ResourceLocation id = new ResourceLocation(BreweryMod.MOD_ID, brewType.id().toLowerCase().replace(" ", "_"));
            Path path = output.getOutputFolder().resolve("data/" + id.getNamespace() + "/brew_types/" + id.getPath() + ".json");
            try {
                Files.createDirectories(path.getParent());
                System.out.println("[BrewTypeProvider] Writing BrewType to: " + path);
                futures.add(DataProvider.saveStable(cache, json, path));
            } catch (Exception e) {
                System.err.println("[BrewTypeProvider] Failed to write BrewType: " + path);
                e.printStackTrace();
            }
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private JsonObject toJson(BrewType brewType) {
        JsonObject json = new JsonObject();
        json.addProperty("maxAlcoholLevel", brewType.maxAlcoholLevel());
        json.addProperty("maxPurity", brewType.maxPurity());
        json.addProperty("tintColor", brewType.tintColor());
        json.addProperty("customLore", brewType.customLore());
        json.addProperty("customName", brewType.customName());
        JsonArray effects = new JsonArray();
        for (MobEffectInstance effect : brewType.effects()) {
            JsonObject eff = new JsonObject();
            eff.addProperty("effect", effect.getEffect().getDescriptionId());
            eff.addProperty("duration", effect.getDuration());
            eff.addProperty("amplifier", effect.getAmplifier());
            effects.add(eff);
        }
        json.add("effects", effects);
        return json;
    }

    @Override
    public @NotNull String getName() {
        return "Brew Types";
    }
}

