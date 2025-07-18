package io.fabianbuthere.brewery.datagen;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.fabianbuthere.brewery.block.ModBlocks;
import io.fabianbuthere.brewery.block.custom.WoodType;
import io.fabianbuthere.brewery.util.BrewType;
import io.fabianbuthere.brewery.util.ItemStackInput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;

import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("removal")
public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput pOutput) {
        super(pOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> pWriter) {
        // Add fermentation barrel recipes for all 11 wood types
        for (WoodType type : WoodType.values()) {
            String wood = type.getSerializedName();
            Item plank = switch (wood) {
                case "acacia" -> Items.ACACIA_PLANKS;
                case "bamboo" -> Items.BAMBOO_PLANKS;
                case "birch" -> Items.BIRCH_PLANKS;
                case "cherry" -> Items.CHERRY_PLANKS;
                case "dark_oak" -> Items.DARK_OAK_PLANKS;
                case "jungle" -> Items.JUNGLE_PLANKS;
                case "mangrove" -> Items.MANGROVE_PLANKS;
                case "crimson" -> Items.CRIMSON_PLANKS;
                case "warped" -> Items.WARPED_PLANKS;
                case "spruce" -> Items.SPRUCE_PLANKS;
                default -> Items.OAK_PLANKS;
            };
            Item slab = switch (wood) {
                case "acacia" -> Items.ACACIA_SLAB;
                case "bamboo" -> Items.BAMBOO_SLAB;
                case "birch" -> Items.BIRCH_SLAB;
                case "cherry" -> Items.CHERRY_SLAB;
                case "dark_oak" -> Items.DARK_OAK_SLAB;
                case "jungle" -> Items.JUNGLE_SLAB;
                case "mangrove" -> Items.MANGROVE_SLAB;
                case "crimson" -> Items.CRIMSON_SLAB;
                case "warped" -> Items.WARPED_SLAB;
                case "spruce" -> Items.SPRUCE_SLAB;
                default -> Items.OAK_SLAB;
            };
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.FERMENTATION_BARRELS.get(type).get())
                .define('P', plank)
                .define('S', slab)
                .define('I', Items.IRON_INGOT)
                .pattern("PSP")
                .pattern("PIP")
                .pattern("PSP")
                .unlockedBy("has_" + wood + "_planks", has(plank))
                .save(pWriter, new ResourceLocation("brewery", "fermentation_barrel_" + wood));
        }

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.BREWING_CAULDRON.get())
            .define('I', Items.COPPER_INGOT)
            .pattern("I I")
            .pattern("I I")
            .pattern("III")
            .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
            .save(pWriter, new ResourceLocation("brewery", "brewing_cauldron"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.DISTILLERY_STATION.get())
                .define('S', Blocks.SMOOTH_STONE)
                .define('G', Items.GLASS)
                .define('L', Items.LAVA_BUCKET)
                .define('W', Items.WATER_BUCKET)
                .define('I', Blocks.IRON_BLOCK)
                .pattern("GLG")
                .pattern("IWI")
                .pattern("SSS")
                .unlockedBy("has_smooth_stone", has(Blocks.SMOOTH_STONE))
                .save(pWriter, new ResourceLocation("brewery", "distillery_station"));
    }

    /**
     * Registers a custom brewing recipe.
     * @param pWriter Consumer for FinishedRecipe
     * @param inputs List of ItemStackInput (item, minCount, maxCount)
     * @param optimalBrewingTime Brewing time in ticks
     * @param maxBrewingTimeError Allowed error in brewing time
     * @param distillingItem Item required for distilling (as a string)
     * @param optimalAgingTime Aging time in ticks
     * @param maxAgingTimeError Allowed error in aging time
     * @param allowedWoodTypes List of allowed wood types (as strings)
     * @param result BrewType result
     */
    private void customBrewing(
            Consumer<FinishedRecipe> pWriter,
            List<ItemStackInput> inputs,
            long optimalBrewingTime,
            float maxBrewingTimeError,
            String distillingItem,
            long optimalAgingTime,
            float maxAgingTimeError,
            List<String> allowedWoodTypes,
            BrewType result
    ) {
        JsonObject json = new JsonObject();
        json.add("inputs", new Gson().toJsonTree(inputs.stream().map(input -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("item", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(input.item()).toString());
            obj.addProperty("minCount", input.minCount());
            obj.addProperty("maxCount", input.maxCount());
            return obj;
        }).toList()));
        json.addProperty("optimalBrewingTime", optimalBrewingTime);
        json.addProperty("maxBrewingTimeError", maxBrewingTimeError);
        json.addProperty("distillingItem", distillingItem);
        json.addProperty("optimalAgingTime", optimalAgingTime);
        json.addProperty("maxAgingTimeError", maxAgingTimeError);
        json.add("allowedWoodTypes", new Gson().toJsonTree(allowedWoodTypes));
        String brewTypeId = result.id();
        json.addProperty("brew_type", brewTypeId);

        pWriter.accept(new FinishedRecipe() {
            @Override
            public void serializeRecipeData(JsonObject jsonOut) {
                jsonOut.add("brewing_data", json);
            }
            @Override
            public ResourceLocation getId() {
                return new ResourceLocation("brewery", result.id().toLowerCase().replace(" ", "_"));
            }
            @Override
            public RecipeSerializer<?> getType() {
                return io.fabianbuthere.brewery.recipe.ModRecipes.BREWING_SERIALIZER.get();
            }
            @Override
            public JsonObject serializeAdvancement() { return null; }
            @Override
            public ResourceLocation getAdvancementId() { return null; }
        });
    }
}
