package io.fabianbuthere.brewery.recipe;

import com.google.gson.*;
import io.fabianbuthere.brewery.config.BreweryConfig;
import io.fabianbuthere.brewery.util.BrewTypeRegistry;
import io.fabianbuthere.brewery.util.ItemStackInput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("removal")
public class BrewingRecipeSerializer implements RecipeSerializer<BrewingRecipe> {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Override
    public BrewingRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
        JsonObject data = pSerializedRecipe.getAsJsonObject("brewing_data");
        // Inputs
        List<ItemStackInput> inputs = new ArrayList<>();
        JsonArray inputsArray = data.getAsJsonArray("inputs");
        for (JsonElement el : inputsArray) {
            JsonObject obj = el.getAsJsonObject();
            String itemId = obj.get("item").getAsString();
            int minCount = obj.get("minCount").getAsInt();
            int maxCount = obj.get("maxCount").getAsInt();
            inputs.add(new ItemStackInput(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new ResourceLocation(itemId)), minCount, maxCount));
        }
        long optimalBrewingTime = data.get("optimalBrewingTime").getAsLong();
        float maxBrewingTimeError = data.get("maxBrewingTimeError").getAsFloat();
        String distillingItem = data.has("distillingItem") ? data.get("distillingItem").getAsString() : "";
        long optimalAgingTime = data.get("optimalAgingTime").getAsLong();
        float maxAgingTimeError = data.get("maxAgingTimeError").getAsFloat();
        List<String> allowedWoodTypes = new ArrayList<>();
        JsonArray woods = data.getAsJsonArray("allowedWoodTypes");
        for (JsonElement el : woods) allowedWoodTypes.add(el.getAsString());
        String brewTypeId = data.get("brew_type").getAsString();
        return new BrewingRecipe(pRecipeId, inputs, optimalBrewingTime, maxBrewingTimeError, distillingItem, optimalAgingTime, maxAgingTimeError, allowedWoodTypes, brewTypeId);
    }

    @Override
    public @Nullable BrewingRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
        int inputCount = pBuffer.readVarInt();
        List<ItemStackInput> inputs = new ArrayList<>();
        for (int i = 0; i < inputCount; i++) {
            ResourceLocation itemId = pBuffer.readResourceLocation();
            int minCount = pBuffer.readVarInt();
            int maxCount = pBuffer.readVarInt();
            inputs.add(new ItemStackInput(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId), minCount, maxCount));
        }
        long optimalBrewingTime = pBuffer.readLong();
        float maxBrewingTimeError = pBuffer.readFloat();
        String distillingItem = pBuffer.readUtf();
        long optimalAgingTime = pBuffer.readLong();
        float maxAgingTimeError = pBuffer.readFloat();
        int woodCount = pBuffer.readVarInt();
        List<String> allowedWoodTypes = new ArrayList<>();
        for (int i = 0; i < woodCount; i++) allowedWoodTypes.add(pBuffer.readUtf());
        String brewTypeId = pBuffer.readUtf();
        return new BrewingRecipe(pRecipeId, inputs, optimalBrewingTime, maxBrewingTimeError, distillingItem, optimalAgingTime, maxAgingTimeError, allowedWoodTypes, brewTypeId);
    }

    @Override
    public void toNetwork(FriendlyByteBuf pBuffer, BrewingRecipe pRecipe) {
        pBuffer.writeVarInt(pRecipe.getInputs().size());
        for (ItemStackInput input : pRecipe.getInputs()) {
            pBuffer.writeResourceLocation(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(input.item()));
            pBuffer.writeVarInt(input.minCount());
            pBuffer.writeVarInt(input.maxCount());
        }
        pBuffer.writeLong(pRecipe.getOptimalBrewingTime());
        pBuffer.writeFloat(pRecipe.getMaxBrewingTimeError());
        pBuffer.writeUtf(pRecipe.getDistillingItem());
        pBuffer.writeLong(pRecipe.getOptimalAgingTime());
        pBuffer.writeFloat(pRecipe.getMaxAgingTimeError());
        pBuffer.writeVarInt(pRecipe.getAllowedWoodTypes().size());
        for (String s : pRecipe.getAllowedWoodTypes()) pBuffer.writeUtf(s);
        pBuffer.writeUtf(pRecipe.getBrewTypeId());
    }
}
