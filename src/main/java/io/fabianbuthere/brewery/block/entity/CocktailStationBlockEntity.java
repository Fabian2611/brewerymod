package io.fabianbuthere.brewery.block.entity;

import io.fabianbuthere.brewery.recipe.CocktailRecipe;
import io.fabianbuthere.brewery.recipe.ModRecipes;
import io.fabianbuthere.brewery.screen.CocktailStationMenu;
import io.fabianbuthere.brewery.util.BrewType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CocktailStationBlockEntity extends BlockEntity implements MenuProvider {

    public static final int BREW_SLOT_COUNT = 4;
    public static final int OUTPUT_SLOT = 4;
    public static final int TOTAL_SLOTS = 5;

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS);

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 60; // 3 seconds at 20 tps

    public CocktailStationBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.COCKTAIL_STATION.get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> CocktailStationBlockEntity.this.progress;
                    case 1 -> CocktailStationBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> CocktailStationBlockEntity.this.progress = pValue;
                    case 1 -> CocktailStationBlockEntity.this.maxProgress = pValue;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.brewery.cocktail_station");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, @NotNull Player pPlayer) {
        return new CocktailStationMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag pTag) {
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.putInt("cocktail_station.progress", this.progress);
        super.saveAdditional(pTag);
    }

    @Override
    public void load(@NotNull CompoundTag pTag) {
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        this.progress = pTag.getInt("cocktail_station.progress");
        super.load(pTag);
    }

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        if (hasRecipe()) {
            progress += 1;
            setChanged(pLevel, pPos, pState);
            if (progress >= maxProgress) {
                craftItem();
                progress = 0;
            }
        } else {
            if (progress > 0) {
                progress = 0;
                setChanged(pLevel, pPos, pState);
            }
        }
    }

    private boolean hasRecipe() {
        ItemStack outputStack = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (!outputStack.isEmpty() && !outputStack.is(Items.AIR)) return false;

        boolean hasAnyBrew = false;
        for (int i = 0; i < BREW_SLOT_COUNT; i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                hasAnyBrew = true;
                break;
            }
        }
        if (!hasAnyBrew) return false;

        // Check if it matches any cocktail recipe
        return findMatchingRecipe().isPresent();
    }

    private Optional<CocktailRecipe> findMatchingRecipe() {
        if (level == null) return Optional.empty();
        SimpleContainer container = new SimpleContainer(TOTAL_SLOTS - 1);
        for (int i = 0; i < TOTAL_SLOTS - 1; i++) {
            container.setItem(i, itemHandler.getStackInSlot(i));
        }
        return level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.COCKTAIL_RECIPE_TYPE).stream()
                .filter(r -> r.matches(container, level))
                .findFirst();
    }

    private void craftItem() {
        Optional<CocktailRecipe> recipeOpt = findMatchingRecipe();
        if (recipeOpt.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, BrewType.GENERIC_FAILED_BREW());
            return;
        }

        CocktailRecipe recipe = recipeOpt.get();
        BrewType resultType = BrewType.getBrewTypeFromId(recipe.getResultBrewTypeId());
        if (resultType == null) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, BrewType.GENERIC_FAILED_BREW());
            return;
        }

        // Calculate average purity from input brews
        int totalPurity = 0;
        int totalMissed = 0;
        int brewCount = 0;
        for (int i = 0; i < BREW_SLOT_COUNT; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == Items.POTION) {
                CompoundTag tag = stack.getTag();
                if (tag != null && tag.contains("CustomPotionEffects")) {
                    int purity = countPurityStars(tag);
                    int missed = countMissedStars(tag);
                    totalPurity += purity;
                    totalMissed += missed;
                    brewCount++;
                }
            }
        }

        int avgPurity = brewCount > 0 ? Math.round((float) totalPurity / brewCount) : 0;
        int avgMissed = brewCount > 0 ? Math.round((float) totalMissed / brewCount) : 0;

        ItemStack result = buildCocktailResult(recipe, resultType, avgPurity, avgMissed);

        itemHandler.setStackInSlot(OUTPUT_SLOT, result);

        for (CocktailRecipe.BrewInput req : recipe.getBrewInputs()) {
            int remaining = req.count();
            for (int i = 0; i < BREW_SLOT_COUNT && remaining > 0; i++) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (BrewType.isBrewType(stack, req.brewTypeId())) {
                    int toRemove = Math.min(remaining, stack.getCount());
                    stack.shrink(toRemove);
                    remaining -= toRemove;
                }
            }
        }
        for (var extra : recipe.getExtras()) {
            int remaining = extra.maxCount();
            for (int i = 0; i < TOTAL_SLOTS - 1 && remaining > 0; i++) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (stack.getItem() == extra.item()) {
                    int toRemove = Math.min(remaining, stack.getCount());
                    stack.shrink(toRemove);
                    remaining -= toRemove;
                }
            }
        }
    }

    private int countPurityStars(CompoundTag tag) {
        CompoundTag display = tag.getCompound("display");
        if (!display.contains("Lore")) return 0;
        var loreList = display.getList("Lore", Tag.TAG_STRING);
        if (loreList.isEmpty()) return 0;
        String firstLore = loreList.getString(0);
        int count = 0;
        for (char c : firstLore.toCharArray()) {
            if (c == '★') count++;
        }
        return count;
    }

    private int countMissedStars(CompoundTag tag) {
        CompoundTag display = tag.getCompound("display");
        if (!display.contains("Lore")) return 0;
        var loreList = display.getList("Lore", Tag.TAG_STRING);
        if (loreList.isEmpty()) return 0;
        String firstLore = loreList.getString(0);
        int count = 0;
        for (char c : firstLore.toCharArray()) {
            if (c == '☆') count++;
        }
        return count;
    }

    private ItemStack buildCocktailResult(CocktailRecipe recipe, BrewType brewType, int effectivePurity, int effectiveMissed) {
        if (level == null) return BrewType.GENERIC_FAILED_BREW();

        int maxPurity = brewType.maxPurity();
        float achievedPurity = (effectiveMissed + effectivePurity) != 0 ? (float)effectivePurity / (float)(effectiveMissed + effectivePurity) : 0.0f;
        int clampedPurity = (int)Math.floor(Math.max(0.0f, Math.min((float)maxPurity * achievedPurity, (float)maxPurity)));
        float purityFactor = maxPurity <= 0 ? 0f : ((float) clampedPurity / (float) maxPurity);

        ItemStack resultItem = new ItemStack(Items.POTION);
        CompoundTag resultTag = resultItem.getOrCreateTag();

        // Purity stars
        String purityRepresentation = "★".repeat(Math.max(0, clampedPurity))
                + "☆".repeat(Math.max(0, maxPurity - clampedPurity));

        resultTag.putString("recipeId", recipe.getId().toString());
        resultTag.putString(BrewType.BREW_TYPE_ID_TAG, brewType.id());
        net.minecraft.nbt.ListTag loreList = new net.minecraft.nbt.ListTag();
        loreList.add(net.minecraft.nbt.StringTag.valueOf(
                net.minecraft.network.chat.Component.Serializer.toJson(
                        net.minecraft.network.chat.Component.literal(purityRepresentation))));
        loreList.add(net.minecraft.nbt.StringTag.valueOf(
                net.minecraft.network.chat.Component.Serializer.toJson(
                        net.minecraft.network.chat.Component.translatable(brewType.customLore()))));

        CompoundTag displayTag = resultTag.getCompound("display");
        displayTag.put("Lore", loreList);
        displayTag.putString("Name",
                net.minecraft.network.chat.Component.Serializer.toJson(
                        net.minecraft.network.chat.Component.translatable(brewType.customName())));
        resultTag.put("display", displayTag);
        resultTag.putInt("CustomPotionColor", brewType.tintColor());

        if (brewType.customTexture() != null && !brewType.customTexture().isEmpty()) {
            resultTag.putString("customTexture", brewType.customTexture());
        }

        java.util.List<net.minecraft.world.effect.MobEffectInstance> resultEffects = new java.util.ArrayList<>();
        for (net.minecraft.world.effect.MobEffectInstance effect : brewType.effects()) {
            net.minecraft.world.effect.MobEffect mobEffect = effect.getEffect();
            int duration = Math.round(effect.getDuration() * purityFactor);
            int amplifier = Math.max(0, Math.round(effect.getAmplifier() * purityFactor));
            if (duration > 0) {
                resultEffects.add(new net.minecraft.world.effect.MobEffectInstance(mobEffect, duration, amplifier));
            }
        }
        // Hangover for bad purity
        if (clampedPurity < (double) maxPurity / 2) {
            int hangoverDuration = 600 * Math.max(1, maxPurity / 2 - clampedPurity + 1);
            resultEffects.add(new net.minecraft.world.effect.MobEffectInstance(
                    io.fabianbuthere.brewery.effect.ModEffects.HANGOVER.get(),
                    hangoverDuration, 0, false, false, true));
        }
        resultTag.put("CustomPotionEffects", BrewType.serializeEffects(resultEffects));

        resultItem.setTag(resultTag);
        return resultItem;
    }
}
