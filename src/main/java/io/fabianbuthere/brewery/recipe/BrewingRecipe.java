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

import java.util.Collections;
import java.util.List;

public class BrewingRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final List<ItemStackInput> inputs;
    private final long optimalBrewingTime;
    private final float maxBrewingTimeError;
    private final String distillingItem;
    private final long optimalAgingTime;
    private final float maxAgingTimeError;
    private final List<String> allowedWoodTypes;
    private final String brewTypeId;

    public BrewingRecipe(ResourceLocation id, List<ItemStackInput> inputs, long optimalBrewingTime,
                         float maxBrewingTimeError, String distillingItem, long optimalAgingTime,
                         float maxAgingTimeError, List<String> allowedWoodTypes, String brewTypeId) {
        this.id = id;
        this.inputs = inputs;
        this.optimalBrewingTime = optimalBrewingTime;
        this.maxBrewingTimeError = maxBrewingTimeError;
        this.distillingItem = distillingItem;
        this.optimalAgingTime = optimalAgingTime;
        this.maxAgingTimeError = maxAgingTimeError;
        this.allowedWoodTypes = allowedWoodTypes;
        this.brewTypeId = brewTypeId;
    }

    public boolean requiresDistilling() {
        return distillingItem != null && !distillingItem.isEmpty();
    }

    public boolean requiresAging() {
        return optimalAgingTime > 0L && !allowedWoodTypes.isEmpty();
    }

    // ── Recipe interface ───────────────────────────

    @Override
    public boolean matches(Container container, Level level) {
        for (ItemStackInput input : inputs) {
            int totalCount = 0;
            for (int j = 0; j < container.getContainerSize(); j++) {
                if (container.getItem(j).getItem() == input.item()) {
                    totalCount += container.getItem(j).getCount();
                }
            }
            if (totalCount < input.minCount() || totalCount > input.maxCount()) {
                return false;
            }
        }
        for (int j = 0; j < container.getContainerSize(); j++) {
            ItemStack stack = container.getItem(j);
            if (!stack.isEmpty() && inputs.stream().noneMatch(i -> i.item() == stack.getItem())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
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
        return ModRecipes.BREWING_RECIPE_TYPE;
    }

    // ── Getters (immutable list returns) ───────────

    public List<ItemStackInput> getInputs() { return Collections.unmodifiableList(inputs); }
    public long getOptimalBrewingTime() { return optimalBrewingTime; }
    public float getMaxBrewingTimeError() { return maxBrewingTimeError; }
    public long getOptimalAgingTime() { return optimalAgingTime; }
    public float getMaxAgingTimeError() { return maxAgingTimeError; }
    public List<String> getAllowedWoodTypes() { return Collections.unmodifiableList(allowedWoodTypes); }
    public String getBrewTypeId() { return brewTypeId; }
    public String getDistillingItem() { return distillingItem; }
}
