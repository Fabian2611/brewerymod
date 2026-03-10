package io.fabianbuthere.brewery.recipe;

import io.fabianbuthere.brewery.util.BrewType;
import io.fabianbuthere.brewery.util.ItemStackInput;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
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

public class CocktailRecipe implements Recipe<Container> {

    public record BrewInput(String brewTypeId, int count) {}

    private final ResourceLocation id;
    private final List<BrewInput> brewInputs;
    private final List<ItemStackInput> extras;
    private final String resultBrewTypeId;

    public CocktailRecipe(ResourceLocation id, List<BrewInput> brewInputs,
                          List<ItemStackInput> extras, String resultBrewTypeId) {
        this.id = id;
        this.brewInputs = brewInputs;
        this.extras = extras;
        this.resultBrewTypeId = resultBrewTypeId;
    }

    @Override
    public boolean matches(Container container, Level level) {
        // Check each brew input requirement
        for (BrewInput req : brewInputs) {
            int found = 0;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (BrewType.isBrewType(stack, req.brewTypeId())) {
                    found += stack.getCount();
                }
            }
            if (found < req.count()) return false;
        }

        // Check each extra (raw item) requirement
        for (ItemStackInput extra : extras) {
            int totalCount = 0;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (stack.getItem() == extra.item()) {
                    totalCount += stack.getCount();
                }
            }
            if (totalCount < extra.minCount() || totalCount > extra.maxCount()) return false;
        }

        // Verify no unexpected items are present
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;

            // Is it a known brew input?
            boolean isBrewInput = false;
            if (stack.getItem() == Items.POTION) {
                CompoundTag tag = stack.getTag();
                if (tag != null && tag.contains("brewTypeId") && tag.contains("CustomPotionEffects")) {
                    for (BrewInput req : brewInputs) {
                        if (BrewType.isBrewType(stack, req.brewTypeId())) {
                            isBrewInput = true;
                            break;
                        }
                    }
                }
            }
            if (isBrewInput) continue;

            boolean isExtra = extras.stream().anyMatch(e -> e.item() == stack.getItem());
            if (!isExtra) return false;
        }

        return true;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return new ItemStack(Items.POTION);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        if (BrewType.isValid(resultBrewTypeId)) {
            return BrewType.getResultItem(resultBrewTypeId);
        }
        return BrewType.DEFAULT_POTION();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.COCKTAIL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.COCKTAIL_RECIPE_TYPE;
    }

    public List<BrewInput> getBrewInputs() { return Collections.unmodifiableList(brewInputs); }
    public List<ItemStackInput> getExtras() { return Collections.unmodifiableList(extras); }
    public String getResultBrewTypeId() { return resultBrewTypeId; }
}
