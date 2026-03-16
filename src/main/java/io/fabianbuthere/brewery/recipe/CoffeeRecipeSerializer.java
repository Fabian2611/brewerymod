package io.fabianbuthere.brewery.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.fabianbuthere.brewery.util.ItemStackInput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("removal")
public class CoffeeRecipeSerializer implements RecipeSerializer<CoffeeRecipe> {

    @Override
    public CoffeeRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
        // Changed to parse "coffee_data" from the JSON to match your request
        JsonObject data = pSerializedRecipe.getAsJsonObject("coffee_data");

        List<CoffeeRecipe.BrewInput> brewInputs = new ArrayList<>();
        JsonArray brewArray = data.getAsJsonArray("brew_inputs");
        for (JsonElement el : brewArray) {
            JsonObject obj = el.getAsJsonObject();
            String brewTypeId = obj.get("brewTypeId").getAsString();
            int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
            brewInputs.add(new CoffeeRecipe.BrewInput(brewTypeId, count));
        }

        List<ItemStackInput> extras = new ArrayList<>();
        if (data.has("extras")) {
            JsonArray extrasArray = data.getAsJsonArray("extras");
            for (JsonElement el : extrasArray) {
                JsonObject obj = el.getAsJsonObject();
                String itemId = obj.get("item").getAsString();
                int minCount = obj.get("minCount").getAsInt();
                int maxCount = obj.get("maxCount").getAsInt();
                extras.add(new ItemStackInput(
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new ResourceLocation(itemId)),
                        minCount, maxCount));
            }
        }

        String resultBrewTypeId = data.get("result_brew_type").getAsString();
        return new CoffeeRecipe(pRecipeId, brewInputs, extras, resultBrewTypeId);
    }

    @Override
    public @Nullable CoffeeRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
        int brewCount = pBuffer.readVarInt();
        List<CoffeeRecipe.BrewInput> brewInputs = new ArrayList<>();
        for (int i = 0; i < brewCount; i++) {
            String brewTypeId = pBuffer.readUtf();
            int count = pBuffer.readVarInt();
            brewInputs.add(new CoffeeRecipe.BrewInput(brewTypeId, count));
        }

        int extraCount = pBuffer.readVarInt();
        List<ItemStackInput> extras = new ArrayList<>();
        for (int i = 0; i < extraCount; i++) {
            ResourceLocation itemId = pBuffer.readResourceLocation();
            int minCount = pBuffer.readVarInt();
            int maxCount = pBuffer.readVarInt();
            extras.add(new ItemStackInput(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId),
                    minCount, maxCount));
        }

        String resultBrewTypeId = pBuffer.readUtf();
        return new CoffeeRecipe(pRecipeId, brewInputs, extras, resultBrewTypeId);
    }

    @Override
    public void toNetwork(FriendlyByteBuf pBuffer, CoffeeRecipe pRecipe) {
        pBuffer.writeVarInt(pRecipe.getBrewInputs().size());
        for (CoffeeRecipe.BrewInput input : pRecipe.getBrewInputs()) {
            pBuffer.writeUtf(input.brewTypeId());
            pBuffer.writeVarInt(input.count());
        }

        pBuffer.writeVarInt(pRecipe.getExtras().size());
        for (ItemStackInput extra : pRecipe.getExtras()) {
            pBuffer.writeResourceLocation(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(extra.item()));
            pBuffer.writeVarInt(extra.minCount());
            pBuffer.writeVarInt(extra.maxCount());
        }

        pBuffer.writeUtf(pRecipe.getResultBrewTypeId());
    }
}
