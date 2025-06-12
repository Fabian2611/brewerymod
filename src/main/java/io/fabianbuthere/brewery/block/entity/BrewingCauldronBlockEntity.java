package io.fabianbuthere.brewery.block.entity;

import io.fabianbuthere.brewery.block.custom.BrewingCauldronBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class BrewingCauldronBlockEntity extends BlockEntity {
    private int currentColor = 0x3F76E4;
    public static final int INVENTORY_SIZE = 3;
    private final ItemStackHandler itemHandler = new ItemStackHandler(INVENTORY_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return canInsert(stack);
        }
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }
    };
    private final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> itemHandler);

    public static final Item[] allowedIngredients = new Item[]{Items.WHEAT, Items.SUGAR, Items.GLOWSTONE_DUST};

    private boolean heated = false;
    public static final Block[] allowedHeatingBlocks = new Block[]{Blocks.FURNACE, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE}; // Replace Items with Blocks as needed

    public BrewingCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BREWING_CAULDRON.get(), pos, state);
    }

    public int getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(int currentColor) {
        this.currentColor = currentColor;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean canInsert(ItemStack stack) {
        if (this.getBlockState().hasProperty(BrewingCauldronBlock.BREW_LEVEL)) {
            int level = this.getBlockState().getValue(BrewingCauldronBlock.BREW_LEVEL);
            if (level != 3) return false;
        } else {
            return false;
        }
        return Arrays.asList(allowedIngredients).contains(stack.getItem());
    }

    public ItemStack insertItemStack(ItemStack stack) {
        if (!canInsert(stack)) return stack;
        ItemStack remaining = stack.copy();
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            remaining = itemHandler.insertItem(i, remaining, false);
            if (remaining.isEmpty()) break;
        }
        return remaining;
    }

    public boolean isHeated() {
        return heated;
    }

    public void updateHeatingState() {
        if (level != null && !level.isClientSide) {
            Block blockBelow = level.getBlockState(worldPosition.below()).getBlock();
            boolean shouldBeHeated = Arrays.asList(allowedHeatingBlocks).contains(blockBelow);
            if (heated != shouldBeHeated) {
                heated = shouldBeHeated;
                setChanged();
            }
        }
    }

    // Called when a glass bottle is used on the cauldron
    public ItemStack getCurrentStateRecipeResult() {
        // TODO: Implement recipe result logic here
        return new ItemStack(Items.GLASS_BOTTLE);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        if (tag.contains("CurrentColor")) this.currentColor = tag.getInt("CurrentColor");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putInt("CurrentColor", currentColor);
    }

    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return handler.cast();
        }
        return super.getCapability(cap, side);
    }
}
