package io.fabianbuthere.brewery.recipe;

import io.fabianbuthere.brewery.block.entity.CoffeeMakerBlockEntity;
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

public class CoffeeRecipe implements Recipe<Container> {

    public record BrewInput(String brewTypeId, int count) {}

    private final ResourceLocation id;
    private final List<BrewInput> brewInputs;
    private final List<ItemStackInput> extras;
    private final String resultBrewTypeId;

    public CoffeeRecipe(ResourceLocation id, List<BrewInput> brewInputs,
                        List<ItemStackInput> extras, String resultBrewTypeId) {
        this.id = id;
        this.brewInputs = brewInputs;
        this.extras = extras;
        this.resultBrewTypeId = resultBrewTypeId;
    }

    @Override
    public boolean matches(Container container, Level level) {
        final int brewStart = CoffeeMakerBlockEntity.BREW_SLOTS_START;
        final int brewEnd = brewStart + CoffeeMakerBlockEntity.BREW_SLOT_COUNT; // exclusive
        final int extraStart = CoffeeMakerBlockEntity.EXTRA_SLOTS_START;
        final int extraEnd = extraStart + CoffeeMakerBlockEntity.EXTRA_SLOT_COUNT; // exclusive

        // 1) Brew slots must contain only potions (or be empty)
        for (int i = brewStart; i < Math.min(container.getContainerSize(), brewEnd); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() != Items.POTION) return false;
        }

        // 2) Extra slots must NOT contain potions (or any brew)
        for (int i = extraStart; i < Math.min(container.getContainerSize(), extraEnd); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == Items.POTION) return false;
        }

        // 3) Brew requirements from brew slots only
        for (BrewInput req : brewInputs) {
            int found = 0;
            for (int i = brewStart; i < Math.min(container.getContainerSize(), brewEnd); i++) {
                ItemStack stack = container.getItem(i);
                if (BrewType.isBrewType(stack, req.brewTypeId())) {
                    found += stack.getCount();
                }
            }
            if (found < req.count()) return false;
        }

        // 4) Extra requirements from extra slots only
        for (ItemStackInput extra : extras) {
            int totalCount = 0;
            for (int i = extraStart; i < Math.min(container.getContainerSize(), extraEnd); i++) {
                ItemStack stack = container.getItem(i);
                if (stack.getItem() == extra.item()) {
                    totalCount += stack.getCount();
                }
            }
            if (totalCount < extra.minCount() || totalCount > extra.maxCount()) return false;
        }

        // 5) Reject unexpected items.
        for (int i = brewStart; i < Math.min(container.getContainerSize(), brewEnd); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;

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
            if (!isBrewInput) return false;
        }

        for (int i = extraStart; i < Math.min(container.getContainerSize(), extraEnd); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;

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
        return ModRecipes.COFFEE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.COFFEE_RECIPE_TYPE;
    }

    public List<BrewInput> getBrewInputs() { return Collections.unmodifiableList(brewInputs); }
    public List<ItemStackInput> getExtras() { return Collections.unmodifiableList(extras); }
    public String getResultBrewTypeId() { return resultBrewTypeId; }
}
