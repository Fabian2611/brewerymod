package io.fabianbuthere.brewery.block.entity;

import io.fabianbuthere.brewery.item.ModItems;
import io.fabianbuthere.brewery.recipe.CoffeeRecipe;
import io.fabianbuthere.brewery.recipe.ModRecipes;
import io.fabianbuthere.brewery.screen.CoffeeMakerMenu;
import io.fabianbuthere.brewery.util.BrewType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

public class CoffeeMakerBlockEntity extends BlockEntity implements MenuProvider {

    // ===== Left side (brewing) =====
    // 0-2: brew slots (potions only)
    public static final int BREW_SLOTS_START = 0;
    public static final int BREW_SLOT_COUNT = 3;

    // 3-4: extras/other slots (any items)  (CHANGED: now 2)
    public static final int EXTRA_SLOTS_START = 3;
    public static final int EXTRA_SLOT_COUNT = 2;

    // 5: left output
    public static final int LEFT_OUTPUT_SLOT = 5;

    // ===== Right side (beans) =====
    // 6: coffee bean input
    public static final int BEAN_INPUT_SLOT = 6;
    // 7: right output
    public static final int BEAN_OUTPUT_SLOT = 7;

    public static final int TOTAL_SLOTS = 8;

    // Timing (20 ticks/sec)
    private static final int LEFT_MAX_PROGRESS = 20 * 30;  // 600
    private static final int RIGHT_MAX_PROGRESS = 20 * 10; // 200

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            CoffeeMakerBlockEntity.this.setChanged();

            if (CoffeeMakerBlockEntity.this.level != null && !CoffeeMakerBlockEntity.this.level.isClientSide) {
                BlockPos p = CoffeeMakerBlockEntity.this.worldPosition;
                BlockState s = CoffeeMakerBlockEntity.this.getBlockState();
                CoffeeMakerBlockEntity.this.level.sendBlockUpdated(p, s, s, 3);
            }
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected final ContainerData data;

    private int leftProgress = 0;
    private int rightProgress = 0;

    private boolean invalidLegacyBlock = false;

    private boolean isLegacyInventorySize(CompoundTag inventoryTag) {
        if (!inventoryTag.contains("Size", Tag.TAG_INT)) return true;
        int savedSize = inventoryTag.getInt("Size");
        return savedSize != TOTAL_SLOTS;
    }

    public CoffeeMakerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.COFFEE_MAKER.get(), pPos, pBlockState);

        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> CoffeeMakerBlockEntity.this.leftProgress;
                    case 1 -> LEFT_MAX_PROGRESS;
                    case 2 -> CoffeeMakerBlockEntity.this.rightProgress;
                    case 3 -> RIGHT_MAX_PROGRESS;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> CoffeeMakerBlockEntity.this.leftProgress = pValue;
                    case 2 -> CoffeeMakerBlockEntity.this.rightProgress = pValue;
                    // max values are constants, ignore writes for 1 and 3
                }
            }

            @Override
            public int getCount() {
                return 4;
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
        return Component.translatable("block.brewery.coffee_maker");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, @NotNull Player pPlayer) {
        return new CoffeeMakerMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag pTag) {
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.putInt("coffee_maker.left_progress", this.leftProgress);
        pTag.putInt("coffee_maker.right_progress", this.rightProgress);
        super.saveAdditional(pTag);
    }

    @Override
    public void load(@NotNull CompoundTag pTag) {
        if (pTag.contains("inventory", Tag.TAG_COMPOUND)) {
            CompoundTag invTag = pTag.getCompound("inventory");

            if (isLegacyInventorySize(invTag)) {
                this.invalidLegacyBlock = true;
            } else {
                itemHandler.deserializeNBT(invTag);
            }
        } else {
            this.invalidLegacyBlock = true;
        }

        this.leftProgress = pTag.getInt("coffee_maker.left_progress");
        this.rightProgress = pTag.getInt("coffee_maker.right_progress");

        super.load(pTag);
    }

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        if (pLevel.isClientSide()) return;
        if (this.invalidLegacyBlock) {
            Containers.dropItemStack(
                    pLevel,
                    pPos.getX() + 0.5, pPos.getY() + 0.5, pPos.getZ() + 0.5,
                    new ItemStack(pState.getBlock().asItem())
            );

            pLevel.removeBlock(pPos, false);

            return;
        }

        // Tick left side (brewing)
        if (hasLeftRecipe()) {
            leftProgress++;
            setChanged(pLevel, pPos, pState);
            if (leftProgress >= LEFT_MAX_PROGRESS) {
                craftLeftItem();
                leftProgress = 0;
            }
        } else if (leftProgress > 0) {
            leftProgress = 0;
            setChanged(pLevel, pPos, pState);
        }

        // Tick right side (beans) - placeholder simple processing
        if (hasRightRecipe()) {
            rightProgress++;
            setChanged(pLevel, pPos, pState);
            if (rightProgress >= RIGHT_MAX_PROGRESS) {
                craftRightItem();
                rightProgress = 0;
            }
        } else if (rightProgress > 0) {
            rightProgress = 0;
            setChanged(pLevel, pPos, pState);
        }
    }

    private boolean hasLeftRecipe() {
        ItemStack outputStack = itemHandler.getStackInSlot(LEFT_OUTPUT_SLOT);
        if (!outputStack.isEmpty() && !outputStack.is(Items.AIR)) return false;

        boolean hasAnyBrew = false;
        for (int i = BREW_SLOTS_START; i < BREW_SLOTS_START + BREW_SLOT_COUNT; i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                hasAnyBrew = true;
                break;
            }
        }
        if (!hasAnyBrew) return false;

        return findMatchingLeftRecipe().isPresent();
    }

    private Optional<CoffeeRecipe> findMatchingLeftRecipe() {
        if (level == null) return Optional.empty();

        // container size = LEFT_OUTPUT_SLOT means indices 0..4 are set (brew+extras)
        SimpleContainer container = new SimpleContainer(LEFT_OUTPUT_SLOT);
        for (int i = 0; i < LEFT_OUTPUT_SLOT; i++) {
            container.setItem(i, itemHandler.getStackInSlot(i));
        }

        return level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.COFFEE_RECIPE_TYPE).stream()
                .filter(r -> r.matches(container, level))
                .findFirst();
    }

    private void craftLeftItem() {
        Optional<CoffeeRecipe> recipeOpt = findMatchingLeftRecipe();
        if (recipeOpt.isEmpty()) {
            itemHandler.setStackInSlot(LEFT_OUTPUT_SLOT, BrewType.GENERIC_FAILED_BREW());
            return;
        }

        CoffeeRecipe recipe = recipeOpt.get();
        BrewType resultType = BrewType.getBrewTypeFromId(recipe.getResultBrewTypeId());
        if (resultType == null) {
            itemHandler.setStackInSlot(LEFT_OUTPUT_SLOT, BrewType.GENERIC_FAILED_BREW());
            return;
        }

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

        ItemStack result = buildCoffeeResult(recipe, resultType, avgPurity, avgMissed);
        itemHandler.setStackInSlot(LEFT_OUTPUT_SLOT, result);

        for (CoffeeRecipe.BrewInput req : recipe.getBrewInputs()) {
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

    private boolean hasRightRecipe() {
        ItemStack out = itemHandler.getStackInSlot(BEAN_OUTPUT_SLOT);
        if (!((!out.isEmpty() && out.getCount() < out.getMaxStackSize()) || out.isEmpty())) return false;

        ItemStack in = itemHandler.getStackInSlot(BEAN_INPUT_SLOT);
        if (in.isEmpty()) return false;

        return true;
    }

    private void craftRightItem() {
        ItemStack in = itemHandler.getStackInSlot(BEAN_INPUT_SLOT);
        if (in.isEmpty()) return;

        ItemStack out = new ItemStack(ModItems.GROUND_COFFEE.get());

        if (itemHandler.getStackInSlot(BEAN_OUTPUT_SLOT).isEmpty()) {
            itemHandler.setStackInSlot(BEAN_OUTPUT_SLOT, out);
        } else {
            ItemStack outStack = itemHandler.getStackInSlot(BEAN_OUTPUT_SLOT);
            int space = outStack.getMaxStackSize() - outStack.getCount();
            if (space > 0 && outStack.getItem() == out.getItem()) {
                int toAdd = Math.min(space, out.getCount());
                outStack.grow(toAdd);
                itemHandler.setStackInSlot(BEAN_OUTPUT_SLOT, outStack);
            }
        }
        in.shrink(1);
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

    private ItemStack buildCoffeeResult(CoffeeRecipe recipe, BrewType brewType, int effectivePurity, int effectiveMissed) {
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
