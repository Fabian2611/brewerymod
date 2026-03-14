package io.fabianbuthere.brewery.block.entity;

import io.fabianbuthere.brewery.recipe.CocktailRecipe;
import io.fabianbuthere.brewery.recipe.ModRecipes;
import io.fabianbuthere.brewery.screen.CocktailStationMenu;
import io.fabianbuthere.brewery.util.BrewType;
import net.minecraft.ChatFormatting;
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

    // Slot layout:
    // 0-3: brew slots (potions only)
    // 4-6: extras/other slots (any items)
    // 7: output
    public static final int BREW_SLOTS_START = 0;
    public static final int BREW_SLOT_COUNT = 4;

    public static final int EXTRA_SLOTS_START = 4;
    public static final int EXTRA_SLOT_COUNT = 3;

    public static final int OUTPUT_SLOT = 7;
    public static final int TOTAL_SLOTS = 8;

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            // CRITICAL: ensures slot changes (including taking output) persist to disk.
            CocktailStationBlockEntity.this.setChanged();

            // Optional but helpful: immediately sync to client.
            if (CocktailStationBlockEntity.this.level != null && !CocktailStationBlockEntity.this.level.isClientSide) {
                BlockPos p = CocktailStationBlockEntity.this.worldPosition;
                BlockState s = CocktailStationBlockEntity.this.getBlockState();
                CocktailStationBlockEntity.this.level.sendBlockUpdated(p, s, s, 3);
            }
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            super.deserializeNBT(nbt);

            // Migration from old worlds where TOTAL_SLOTS was 5:
            // 0-3 inputs, 4 output.
            if (this.getSlots() < TOTAL_SLOTS) {
                ItemStackHandler old = new ItemStackHandler(this.getSlots());
                old.deserializeNBT(nbt);

                // Force resize to new TOTAL_SLOTS
                CompoundTag resized = nbt.copy();
                resized.putInt("Size", TOTAL_SLOTS);
                super.deserializeNBT(resized);

                // old 0-3 -> new 0-3
                for (int i = 0; i < Math.min(4, old.getSlots()); i++) {
                    this.setStackInSlot(i, old.getStackInSlot(i));
                }
                // old output slot 4 -> new output slot 7 (if it existed)
                if (old.getSlots() > 4) {
                    this.setStackInSlot(OUTPUT_SLOT, old.getStackInSlot(4));
                }
            } else {
                // Safety: if an older "expanded" version stored output in slot 4, move it to new output slot 7.
                if (OUTPUT_SLOT != 4 && this.getStackInSlot(OUTPUT_SLOT).isEmpty() && !this.getStackInSlot(4).isEmpty()) {
                    this.setStackInSlot(OUTPUT_SLOT, this.getStackInSlot(4));
                    this.setStackInSlot(4, ItemStack.EMPTY);
                }
            }
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 200;

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
        // Let the handler's deserializeNBT handle migration/resize.
        if (pTag.contains("inventory", Tag.TAG_COMPOUND)) {
            itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        }
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
        for (int i = BREW_SLOTS_START; i < BREW_SLOTS_START + BREW_SLOT_COUNT; i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                hasAnyBrew = true;
                break;
            }
        }
        if (!hasAnyBrew) return false;

        return findMatchingRecipe().isPresent();
    }

    private Optional<CocktailRecipe> findMatchingRecipe() {
        if (level == null) return Optional.empty();

        // Inputs only: 0-6 (brew + extras), exclude output (7)
        SimpleContainer container = new SimpleContainer(OUTPUT_SLOT);
        for (int i = 0; i < OUTPUT_SLOT; i++) {
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

        // Average purity from brew slots only (0-3)
        int totalPurity = 0;
        int totalMissed = 0;
        int brewCount = 0;
        for (int i = BREW_SLOTS_START; i < BREW_SLOTS_START + BREW_SLOT_COUNT; i++) {
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

        // Consume brews from brew slots only (0-3)
        for (CocktailRecipe.BrewInput req : recipe.getBrewInputs()) {
            int remaining = req.count();
            for (int i = BREW_SLOTS_START; i < BREW_SLOTS_START + BREW_SLOT_COUNT && remaining > 0; i++) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (BrewType.isBrewType(stack, req.brewTypeId())) {
                    int toRemove = Math.min(remaining, stack.getCount());
                    stack.shrink(toRemove);
                    remaining -= toRemove;
                }
            }
        }

        // Consume extras from extra slots only (4-6)
        for (var extra : recipe.getExtras()) {
            int remaining = extra.maxCount();
            for (int i = EXTRA_SLOTS_START; i < EXTRA_SLOTS_START + EXTRA_SLOT_COUNT && remaining > 0; i++) {
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
        float achievedPurity = (effectiveMissed + effectivePurity) != 0 ? (float) effectivePurity / (float) (effectiveMissed + effectivePurity) : 0.0f;
        int clampedPurity = (int) Math.floor(Math.max(0.0f, Math.min((float) maxPurity * achievedPurity, (float) maxPurity)));
        float purityFactor = maxPurity <= 0 ? 0f : ((float) clampedPurity / (float) maxPurity);

        ItemStack resultItem = new ItemStack(Items.POTION);
        CompoundTag resultTag = resultItem.getOrCreateTag();

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

        if (clampedPurity < (double) maxPurity / 2) {
            int hangoverDuration = 600 * Math.max(1, maxPurity / 2 - clampedPurity + 1);
            resultEffects.add(new net.minecraft.world.effect.MobEffectInstance(
                    io.fabianbuthere.brewery.effect.ModEffects.HANGOVER.get(),
                    hangoverDuration, 0, false, false, true));
        }
        resultTag.put("CustomPotionEffects", BrewType.serializeEffects(resultEffects));

        resultItem.setTag(resultTag);
        resultItem.setHoverName(Component.translatable(brewType.customName())
                .withStyle(style -> style.withItalic(false).withColor(ChatFormatting.YELLOW)));
        return resultItem;
    }
}
