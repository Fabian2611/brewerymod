package io.fabianbuthere.brewery.recipe;

import io.fabianbuthere.brewery.util.BrewType;
import io.fabianbuthere.brewery.util.ItemStackInput;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class BrewingRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final List<ItemStackInput> inputs;
    private final long optimalBrewingTime;
    private final float maxBrewingTimeError;
    private final boolean needsDistilling;
    private final long optimalAgingTime;
    private final float maxAgingTimeError;
    private final List<String> allowedWoodTypes;
    private final String brewTypeId;

    public BrewingRecipe(ResourceLocation id, List<ItemStackInput> inputs, long optimalBrewingTime, float maxBrewingTimeError, boolean needsDistilling, long optimalAgingTime, float maxAgingTimeError, List<String> allowedWoodTypes, String brewTypeId) {
        this.id = id;
        this.inputs = inputs;
        this.optimalBrewingTime = optimalBrewingTime;
        this.maxBrewingTimeError = maxBrewingTimeError;
        this.needsDistilling = needsDistilling;
        this.optimalAgingTime = optimalAgingTime;
        this.maxAgingTimeError = maxAgingTimeError;
        this.allowedWoodTypes = allowedWoodTypes;
        this.brewTypeId = brewTypeId;
    }

    @Override
    public boolean matches(Container container, Level level) {
        // Simple matching: check if all required items are present in the right amounts
        for (int i = 0; i < inputs.size(); i++) {
            ItemStackInput input = inputs.get(i);
            boolean found = false;
            for (int j = 0; j < container.getContainerSize(); j++) {
                if (container.getItem(j).getItem() == input.item() &&
                    container.getItem(j).getCount() >= input.minCount()) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        // Return a placeholder (should be replaced with actual brewed item logic)
        return new ItemStack(Items.GLASS_BOTTLE);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        if (BrewType.isValid(brewTypeId)) {
            return BrewType.getResultItem(brewTypeId);
        }
        return BrewType.DEFAULT_POTION();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.BREWING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.BREWING_TYPE;
    }

    public List<ItemStackInput> getInputs() { return inputs; }
    public long getOptimalBrewingTime() { return optimalBrewingTime; }
    public float getMaxBrewingTimeError() { return maxBrewingTimeError; }
    public boolean needsDistilling() { return needsDistilling; }
    public long getOptimalAgingTime() { return optimalAgingTime; }
    public float getMaxAgingTimeError() { return maxAgingTimeError; }
    public List<String> getAllowedWoodTypes() { return allowedWoodTypes; }
    public String getBrewTypeId() { return brewTypeId; }
}
