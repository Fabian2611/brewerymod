package io.fabianbuthere.brewery.block.entity;

import io.fabianbuthere.brewery.block.custom.FermentationBarrelBlock;
import io.fabianbuthere.brewery.screen.DistilleryStationMenu;
import io.fabianbuthere.brewery.screen.FermentationBarrelMenu;
import io.fabianbuthere.brewery.util.BrewType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("removal")
public class FermentationBarrelBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(9) {
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack stack = super.extractItem(slot, amount, simulate);
            if (!simulate && !stack.isEmpty()) {
                setChanged();
                return getResultItem(stack, progresses[slot]);
            }
            return stack;
        }
    };

    private @NotNull ItemStack getResultItem(ItemStack stack, long progress) {
        if (level == null || !stack.hasTag() || stack.isEmpty()) return stack;

        String recipeId = stack.getOrCreateTag().getString("recipeId");
        if (recipeId.isEmpty()) return stack;

        ResourceLocation rl = new ResourceLocation(recipeId);
        var recipeOpt = level.getRecipeManager().byKey(rl);
        if (recipeOpt.isPresent() && recipeOpt.get() instanceof io.fabianbuthere.brewery.recipe.BrewingRecipe brewingRecipe) {
            // Fail if distillery is needed but missing
            if (!(brewingRecipe.getDistillingItem() == null) && !brewingRecipe.getDistillingItem().isEmpty()) {
                CompoundTag tag = stack.getOrCreateTag();
                if (!tag.getString("distillingItem").equals(brewingRecipe.getDistillingItem())) {
                    return BrewType.GENERIC_FAILED_BREW();
                }
            }
            // Fail if brew does not need aging
            if (brewingRecipe.getOptimalAgingTime() == 0L || brewingRecipe.getAllowedWoodTypes().isEmpty()) return BrewType.INCORRECT_AGING_BREW();
            // Fail if wood type does not match
            String woodType = getBlockState().getValue(FermentationBarrelBlock.WOOD_TYPE).getSerializedName();
            if (!brewingRecipe.getAllowedWoodTypes().contains(woodType)) return BrewType.INCORRECT_AGING_BREW();
            // Fail if time was off
            float maxError = brewingRecipe.getMaxAgingTimeError();
            long maxTime = Math.round(brewingRecipe.getOptimalAgingTime() * (1 + maxError));
            long minTime = Math.round(brewingRecipe.getOptimalAgingTime() * (1 - maxError));
            if (progress < minTime || progress > maxTime) {return BrewType.INCORRECT_AGING_BREW();}
            // Return the resulting brew
            float error = (float) Math.abs(progress - brewingRecipe.getOptimalAgingTime()) / brewingRecipe.getOptimalAgingTime();
            ItemStack resultItem = brewingRecipe.getResultItem(level.registryAccess());
            // Get the result BrewType from the recipe
            BrewType brewTypeResult = BrewType.getResultBrewType(brewingRecipe.getBrewTypeId());

            int maxPurity = brewTypeResult.maxPurity();
            int actualPurity = stack.getOrCreateTag().getInt("purity");

            // Calculate purity factor: higher purity = less loss, lower purity = more loss
            // Make error contribution stronger (e.g., square the error)
            float errorContribution = (float) Math.pow(1.0f - error, 2);
            // Calculate effective purity as an integer, influenced by errorContribution
            int effectivePurity = Math.round(actualPurity * errorContribution);
            float purityFactor = (float) effectivePurity / (float) maxPurity;

            StringBuilder purityRepresentation = new StringBuilder();
            purityRepresentation.append("★".repeat(Math.max(0, effectivePurity)));
            purityRepresentation.append("☆".repeat(Math.max(0, maxPurity - effectivePurity)));

            CompoundTag resultTag = resultItem.getOrCreateTag();
            resultTag.putString("recipeId", recipeId);

            ListTag loreList = new ListTag();
            loreList.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(purityRepresentation.toString()))));
            loreList.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable(brewTypeResult.customLore()))));
            CompoundTag displayTag = resultTag.getCompound("display");
            displayTag.put("Lore", loreList);
            displayTag.putString("Name", Component.Serializer.toJson(Component.translatable(brewTypeResult.customName())));
            resultTag.put("display", displayTag);
            resultItem.setTag(resultTag);

            // Modify amplifier and duration with the effective purity factor
            List<MobEffectInstance> resultEffects = new java.util.ArrayList<>();
            for (MobEffectInstance effect : brewTypeResult.effects()) {
                MobEffect mobEffect = effect.getEffect();
                int duration = Math.round(effect.getDuration() * purityFactor);
                int amplifier = Math.max(0, Math.round(effect.getAmplifier() * purityFactor));
                if (duration > 0) {
                    resultEffects.add(new MobEffectInstance(mobEffect, duration, amplifier));
                }
            }
            // Add hangover effect for bad purity
            if (effectivePurity < (double)maxPurity / 2) {
                resultEffects.add(new MobEffectInstance(io.fabianbuthere.brewery.effect.ModEffects.HANGOVER.get(), 600 * (Math.max(1, maxPurity / 2 - effectivePurity + 1)), 0, false, false, true));
            }

            resultItem.getTag().put("CustomPotionEffects", BrewType.serializeEffects(resultEffects));

            return resultItem;
        }
        return stack;
    }

    private long[] progresses = new long[9];

    protected final ContainerData data;
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    // Define constants for data indices
    private static final int DATA_PROGRESS_0 = 0;
    private static final int DATA_PROGRESS_1 = 1;
    private static final int DATA_PROGRESS_2 = 2;
    private static final int DATA_PROGRESS_3 = 3;
    private static final int DATA_PROGRESS_4 = 4;
    private static final int DATA_PROGRESS_5 = 5;
    private static final int DATA_PROGRESS_6 = 6;
    private static final int DATA_PROGRESS_7 = 7;
    private static final int DATA_PROGRESS_8 = 8;
    private static final int DATA_SIZE = 9;

    public FermentationBarrelBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.FERMENTATION_BARRELS.get(
            pBlockState.getValue(FermentationBarrelBlock.WOOD_TYPE)).get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                if (pIndex >= 0 && pIndex < progresses.length) {
                    // Clamp to int range for syncing
                    return (int)Math.min(progresses[pIndex], Integer.MAX_VALUE);
                }
                return 0;
            }

            @Override
            public void set(int pIndex, int pValue) {
                if (pIndex >= 0 && pIndex < progresses.length) {
                    progresses[pIndex] = pValue;
                }
            }

            @Override
            public int getCount() {
                return DATA_SIZE;
            }
        };
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.putLongArray("fermentation_barrel.progresses", progresses);
        super.saveAdditional(pTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        progresses = pTag.getLongArray("fermentation_barrel.progresses");
        super.load(pTag);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.brewery.fermentation_barrel");
    }

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        for (int i = 0; i < progresses.length; i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                if (hasRecipe(itemHandler.getStackInSlot(i))) {
                    progresses[i] += 1;
                    setChanged(pLevel, pPos, pState);
                } else {
                    progresses[i] = 0;
                }
            } else {
                progresses[i] = 0;
            }
        }
    }

    private boolean hasRecipe(@NotNull ItemStack stackInSlot) {
        return !stackInSlot.getOrCreateTag().getString("recipeId").isEmpty();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new FermentationBarrelMenu(pContainerId, pPlayerInventory, this, this.data);
    }
}
