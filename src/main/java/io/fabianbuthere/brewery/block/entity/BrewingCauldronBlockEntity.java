package io.fabianbuthere.brewery.block.entity;

import io.fabianbuthere.brewery.block.custom.BrewingCauldronBlock;
import io.fabianbuthere.brewery.recipe.BrewingRecipe;
import io.fabianbuthere.brewery.recipe.ModRecipes;
import io.fabianbuthere.brewery.util.BrewType;
import io.fabianbuthere.brewery.util.UtilMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
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
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public class BrewingCauldronBlockEntity extends BlockEntity {
    public static final int DEFAULT_COLOR = 0x3F76E4;
    public static final int DEFAULT_FAILED_COLOR = 0x6580b5;
    private int currentColor = DEFAULT_COLOR;
    private int syncedBrewingTicks = 0;
    private int syncedCurrentColor = DEFAULT_COLOR;
    public static final int INVENTORY_SIZE = 3;
    private boolean inventoryChanged = false;
    private final ItemStackHandler itemHandler = new ItemStackHandler(INVENTORY_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            inventoryChanged = true;
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

    public static final Item[] BLACKLISTED_INGREDIENTS = Set.of(new Item[]{
            Items.WATER_BUCKET, Items.LAVA_BUCKET, Items.GLASS_BOTTLE, Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION, Items.BUCKET
    }).toArray(new Item[0]);

    private boolean heated = false;
    public static final Block[] allowedHeatingBlocks = new Block[]{Blocks.MAGMA_BLOCK, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE, Blocks.LAVA, Blocks.FIRE, Blocks.SOUL_FIRE}; // Replace Items with Blocks as needed

    private BrewingRecipe lockedRecipe = null;
    private int brewingTicks = 0;
    private boolean reValidateRecipe = false;

    public BrewingCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BREWING_CAULDRON.get(), pos, state);
    }

    public int getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(int currentColor) {
        if (this.currentColor != currentColor) {
            this.currentColor = currentColor;
            setChanged();
            if (level != null) {
                if (!level.isClientSide) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    level.blockEvent(worldPosition, getBlockState().getBlock(), 0, 0);
                }
            }
        }
    }

    public boolean canInsert(ItemStack stack) {
        if (this.getBlockState().hasProperty(BrewingCauldronBlock.BREW_LEVEL)) {
            int level = this.getBlockState().getValue(BrewingCauldronBlock.BREW_LEVEL);
            if (level != 3) return false;
        } else {
            return false;
        }
        return !Arrays.asList(BLACKLISTED_INGREDIENTS).contains(stack.getItem()) && !stack.isEmpty() && !(stack.getItem() instanceof SpawnEggItem);
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

    public void serverTick(Level level) {
        if (level.isClientSide) return;

        // Reset brewing if inventory is empty
        boolean inventoryEmpty = true;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                inventoryEmpty = false;
                break;
            }
        }
        if (inventoryEmpty) {
            resetBrewing();
            return;
        }

        if (reValidateRecipe) {
            if (getBlockState().getValue(BrewingCauldronBlock.BREW_LEVEL) == 0) {
                resetBrewing();
                return;
            }
        }

        // Early return if not heated
        if (!isHeated()) {
            if (lockedRecipe != null || brewingTicks != 0) {
                resetBrewing();
            }
            return;
        }

        // Only revalidate recipe if inventory changed
        if (inventoryChanged) {
            inventoryChanged = false;
            SimpleContainer container = new SimpleContainer(itemHandler.getSlots());
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                container.setItem(i, itemHandler.getStackInSlot(i));
            }
            Optional<BrewingRecipe> match = level.getRecipeManager().getAllRecipesFor(ModRecipes.BREWING_RECIPE_TYPE).stream()
                .filter(r -> r.matches(container, level)).findFirst();
            if (match.isPresent()) {
                lockedRecipe = match.get();
                brewingTicks = 0;
                setChanged();
            } else {
                // Only clear inventory if cauldron is empty
                if (getBlockState().getValue(BrewingCauldronBlock.BREW_LEVEL) == 0) {
                    resetBrewing();
                } else {
                    // Just reset brewing state, keep inventory
                    lockedRecipe = null;
                    brewingTicks = 0;
                    setCurrentColor(DEFAULT_COLOR);
                    setChanged();
                }
                return;
            }
        }

        if (lockedRecipe != null) {
            brewingTicks++;
            BrewType brewType = BrewType.getBrewTypeFromId(lockedRecipe.getBrewTypeId());
            int prevColor = currentColor;
            if (brewingTicks <= lockedRecipe.getOptimalBrewingTime()) {
                double progress = (double) brewingTicks / lockedRecipe.getOptimalBrewingTime();
                progress = Math.max(0.0, Math.min(1.0, progress));
                if (brewType != null) {
                    setCurrentColor(UtilMath.lerpColor(DEFAULT_COLOR, brewType.tintColor(), (float)progress));
                } else {
                    setCurrentColor(DEFAULT_COLOR);
                }
            } else {
                double maxTicks = lockedRecipe.getOptimalBrewingTime() * (1 + lockedRecipe.getMaxBrewingTimeError());
                if (brewingTicks <= maxTicks) {
                    double revertProgress = (brewingTicks - lockedRecipe.getOptimalBrewingTime()) / (maxTicks - lockedRecipe.getOptimalBrewingTime());
                    revertProgress = Math.max(0.0, Math.min(1.0, revertProgress));
                    if (brewType != null) {
                        setCurrentColor(UtilMath.lerpColor(brewType.tintColor(), DEFAULT_FAILED_COLOR, (float)revertProgress));
                    } else {
                        setCurrentColor(DEFAULT_COLOR);
                    }
                } else {
                    setCurrentColor(DEFAULT_FAILED_COLOR);
                }
            }
            // Only call setChanged if color actually changed
            if (currentColor != prevColor) {
                setChanged();
            }
        }
    }

    // Client-side tick for color animation and particle spawning
    public void clientTick(Level level) {
        if (level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            // Use syncedBrewingTicks and syncedCurrentColor for client-side effects
            if (syncedBrewingTicks > 0 && level.getGameTime() % 5 == 0) {
                double x = worldPosition.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6;
                double y = worldPosition.getY() + 1.2;
                double z = worldPosition.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6;
                float r = ((syncedCurrentColor >> 16) & 0xFF) / 255.0f;
                float g = ((syncedCurrentColor >> 8) & 0xFF) / 255.0f;
                float b = (syncedCurrentColor & 0xFF) / 255.0f;
                if (r == 0.0f && g == 0.0f && b == 0.0f) {
                    r = g = b = 0.001f;
                }
                float size = 1.0f;
                level.addParticle(new DustParticleOptions(new Vector3f(r, g, b), size), x, y, z, 0, 0.2, 0);
            }
        }
    }

    public BrewingRecipe getLockedRecipe() {
        return lockedRecipe;
    }

    public int getBrewingTicks() {
        return brewingTicks;
    }

    public void resetBrewing() {
        setCurrentColor(DEFAULT_COLOR);
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        setReValidateRecipe();
        lockedRecipe = null;
        brewingTicks = 0;
        setChanged();
    }

    // Called when a glass bottle is used on the cauldron
    public ItemStack getCurrentStateRecipeResult() {
        if (lockedRecipe == null) return BrewType.DEFAULT_POTION();
        BrewType baseResult = BrewType.getBrewTypeFromId(lockedRecipe.getBrewTypeId());
        if (baseResult == null) return BrewType.DEFAULT_POTION();
        return baseResult.toItem(lockedRecipe, brewingTicks, itemHandler);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        if (tag.contains("CurrentColor")) this.currentColor = tag.getInt("CurrentColor");
        if (tag.contains("BrewingTicks")) this.syncedBrewingTicks = tag.getInt("BrewingTicks");
        if (tag.contains("CurrentWaterColor")) this.syncedCurrentColor = tag.getInt("CurrentWaterColor");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putInt("CurrentColor", currentColor);
        tag.putInt("BrewingTicks", brewingTicks);
        tag.putInt("CurrentWaterColor", currentColor);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("CurrentColor", currentColor);
        tag.putInt("BrewingTicks", brewingTicks);
        tag.putInt("CurrentWaterColor", currentColor); // Sync the animated/interpolated color
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (tag.contains("CurrentColor")) {
            this.currentColor = tag.getInt("CurrentColor");
        }
        if (tag.contains("BrewingTicks")) {
            this.syncedBrewingTicks = tag.getInt("BrewingTicks");
        }
        if (tag.contains("CurrentWaterColor")) {
            this.syncedCurrentColor = tag.getInt("CurrentWaterColor");
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        handleUpdateTag(pkt.getTag());
    }

    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return handler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        // Called when sendBlockEntityEvent is triggered
        if (level != null && level.isClientSide) {
            setChanged();
        }
        return super.triggerEvent(id, type);
    }

    public void setReValidateRecipe() {
        this.reValidateRecipe = true;
        updateHeatingState();
        setChanged();
    }
}
