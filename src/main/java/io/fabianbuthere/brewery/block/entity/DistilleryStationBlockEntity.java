package io.fabianbuthere.brewery.block.entity;

import io.fabianbuthere.brewery.screen.DistilleryStationMenu;
import io.fabianbuthere.brewery.util.BrewType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
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

public class DistilleryStationBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {

    };

    private static final int INPUT_SLOT = 0;
    private static final int FILTER_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 200;

    public DistilleryStationBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.DISTILLERY_STATION.get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> DistilleryStationBlockEntity.this.progress;
                    case 1 -> DistilleryStationBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> DistilleryStationBlockEntity.this.progress = pValue;
                    case 1 -> DistilleryStationBlockEntity.this.maxProgress = pValue;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer((itemHandler.getSlots()));
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
    public Component getDisplayName() {
        return Component.translatable("block.brewery.distillery_station");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new DistilleryStationMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.putInt("distillery_station.progress", this.progress);

        super.saveAdditional(pTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        this.progress = pTag.getInt("distillery_station.progress");

        super.load(pTag);
    }

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        // Explicitly cancel progress if filter is removed during processing
        if (itemHandler.getStackInSlot(FILTER_SLOT).isEmpty()) {
            if (progress > 0) {
                progress = 0;
                setChanged(pLevel, pPos, pState);
            }
            return;
        }
        if (hasRecipe()) {
            progress += 1;
            setChanged(pLevel, pPos, pState);

            if(progress >= maxProgress) {
                craftItem();
                progress = 0;
            }
        } else {
            progress = 0;
        }
    }

    private boolean hasRecipe() {
        ItemStack inputStack = itemHandler.getStackInSlot(INPUT_SLOT);
        if (inputStack.isEmpty()) return false;

        ItemStack filterStack = itemHandler.getStackInSlot(FILTER_SLOT);
        if (filterStack.isEmpty()) return false;

        ItemStack outputStack = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (!outputStack.isEmpty() && !outputStack.is(Items.AIR)) return false;

        if (inputStack.getItem() == net.minecraft.world.item.Items.POTION) {
            CompoundTag tag = inputStack.getTag();
            if (tag == null) return false;
            if (tag.contains("recipeId")) {
                String recipeId = tag.getString("recipeId");
                return !recipeId.isEmpty();
            }
        }
        return false;
    }

    private void craftItem() {
        ItemStack inputStack = itemHandler.getStackInSlot(INPUT_SLOT);
        ItemStack filterStack = itemHandler.getStackInSlot(FILTER_SLOT);

        CompoundTag inputTag = inputStack.getTag();
        if (inputTag != null && inputTag.contains("recipeId")) {
            String recipeId = inputTag.getString("recipeId");
            if (level != null && !recipeId.isEmpty()) {
                var recipes = level.getRecipeManager().getAllRecipesFor(io.fabianbuthere.brewery.recipe.ModRecipes.BREWING_TYPE);
                for (var recipe : recipes) {
                    if (recipe.getId().toString().equals(recipeId)) {
                        ItemStack result = BrewType.finalizeBrew(
                            recipe,
                            inputStack,
                            filterStack,
                            0L, // No aging/distilling progress needed for distillery
                            null, // No barrel wood type for distillery
                            "distillery",
                            level
                        );
                        itemHandler.setStackInSlot(OUTPUT_SLOT, result);
                        inputStack.shrink(1);
                        filterStack.shrink(1);
                        return;
                    }
                }
            }
        } else {
            ItemStack result = BrewType.GENERIC_FAILED_BREW();
            itemHandler.setStackInSlot(OUTPUT_SLOT, result);
        }
    }
}
