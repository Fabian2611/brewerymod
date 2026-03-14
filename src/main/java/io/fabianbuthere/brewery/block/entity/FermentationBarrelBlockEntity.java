package io.fabianbuthere.brewery.block.entity;

import io.fabianbuthere.brewery.block.custom.FermentationBarrelBlock;
import io.fabianbuthere.brewery.recipe.BrewingRecipe;
import io.fabianbuthere.brewery.screen.FermentationBarrelMenu;
import io.fabianbuthere.brewery.util.BrewType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
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

@SuppressWarnings("removal")
public class FermentationBarrelBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            FermentationBarrelBlockEntity.this.setChanged();
        }
    };

    private static final long MS_PER_SECOND = 1000L;
    private static final double TICKS_PER_SECOND = 20.0;

    private long[] startTimesMs = new long[9];

    private @NotNull ItemStack getResultItem(ItemStack stack, long elapsedTicks) {
        if (level == null || !stack.hasTag() || stack.isEmpty()) return stack;

        String recipeId = stack.getOrCreateTag().getString("recipeId");
        if (recipeId.isEmpty()) return stack;

        ResourceLocation rl = new ResourceLocation(recipeId);
        var recipeOpt = level.getRecipeManager().byKey(rl);
        if (recipeOpt.isPresent() && recipeOpt.get() instanceof BrewingRecipe brewingRecipe) {
            String woodType = getBlockState().getValue(FermentationBarrelBlock.WOOD_TYPE).getSerializedName();
            if (level.isClientSide) {
                return stack;
            }

            return BrewType.finalizeBarrelBrew(
                    brewingRecipe,
                    stack,
                    elapsedTicks,
                    woodType,
                    level
            );
        }
        return stack;
    }

    public long getElapsedTicksForSlot(int slot) {
        if (slot < 0 || slot >= startTimesMs.length) return 0L;
        long start = startTimesMs[slot];
        if (start <= 0L) return 0L;
        long now = System.currentTimeMillis();
        long elapsedMs = Math.max(0L, now - start);
        return (long) Math.floor((elapsedMs * TICKS_PER_SECOND) / 1000.0);
    }

    public long getElapsedSecondsForSlot(int slot) {
        if (slot < 0 || slot >= startTimesMs.length) return 0L;
        long start = startTimesMs[slot];
        if (start <= 0L) return 0L;
        long now = System.currentTimeMillis();
        long elapsedMs = Math.max(0L, now - start);
        return elapsedMs / MS_PER_SECOND;
    }

    public static @NotNull String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds / 60) % 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "min " + seconds + "s";
        }
        return minutes + "min " + seconds + "s";
    }

    public @NotNull Component buildClockStatusMessage() {
        int active = 0;
        long min = Long.MAX_VALUE;
        long max = 0L;
        int singleSlot = -1;
        long singleTime = 0L;

        for (int i = 0; i < startTimesMs.length; i++) {
            if (startTimesMs[i] <= 0L) continue;

            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty() || stack.getOrCreateTag().getString("recipeId").isEmpty()) {
                continue;
            }

            long t = getElapsedSecondsForSlot(i);
            active++;

            if (t < min) min = t;
            if (t > max) max = t;

            singleSlot = i;
            singleTime = t;
        }

        if (active == 0) {
            return Component.literal("empty");
        }
        if (active == 1) {
            return Component.literal("Slot " + (singleSlot + 1) + ": " + formatDuration(singleTime));
        }
        return Component.literal(active + " slots: min " + formatDuration(min) + " / max " + formatDuration(max));
    }

    public @NotNull ItemStack finalizeStackFromSlot(int slot, @NotNull ItemStack original) {
        long elapsedTicks = getElapsedTicksForSlot(slot);
        return getResultItem(original, elapsedTicks);
    }

    protected final ContainerData data;
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    private static final int DATA_SIZE = 9;

    public FermentationBarrelBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.FERMENTATION_BARRELS.get(
                pBlockState.getValue(FermentationBarrelBlock.WOOD_TYPE)).get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return 0;
            }

            @Override
            public void set(int pIndex, int pValue) {

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
        pTag.putLongArray("fermentation_barrel.startTimesMs", startTimesMs);
        super.saveAdditional(pTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        long[] loaded = pTag.getLongArray("fermentation_barrel.startTimesMs");
        if (loaded != null && loaded.length == 9) {
            startTimesMs = loaded;
        } else {
            startTimesMs = new long[9];
            if (loaded != null) {
                System.arraycopy(loaded, 0, startTimesMs, 0, Math.min(loaded.length, startTimesMs.length));
            }
        }
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
        long now = System.currentTimeMillis();
        boolean changed = false;

        for (int i = 0; i < startTimesMs.length; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);

            if (stack.isEmpty()) {
                if (startTimesMs[i] != 0L) {
                    startTimesMs[i] = 0L;
                    changed = true;
                }
                continue;
            }

            if (!hasRecipe(stack)) {
                if (startTimesMs[i] != 0L) {
                    startTimesMs[i] = 0L;
                    changed = true;
                }
                continue;
            }

            if (startTimesMs[i] == 0L) {
                startTimesMs[i] = now;
                changed = true;
            }
        }

        if (changed) {
            setChanged(pLevel, pPos, pState);
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
