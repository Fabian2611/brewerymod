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

    // Client-synced display state
    private int syncedBrewingTicks = 0;
    private int syncedCurrentColor = DEFAULT_COLOR;
    private int syncedBrewColor = DEFAULT_COLOR;

    public static final int INVENTORY_SIZE = 5;
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
            Items.WATER_BUCKET, Items.LAVA_BUCKET, Items.GLASS_BOTTLE, Items.POTION,
            Items.SPLASH_POTION, Items.LINGERING_POTION, Items.BUCKET
    }).toArray(new Item[0]);

    private boolean heated = false;
    public static final Block[] allowedHeatingBlocks = new Block[]{
            Blocks.MAGMA_BLOCK, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE,
            Blocks.LAVA, Blocks.FIRE, Blocks.SOUL_FIRE
    };

    private BrewingRecipe lockedRecipe = null;

    /**
     * REALTIME source of truth: store only start time.
     * 0 => not brewing / timer not running.
     */
    private long brewingStartMs = 0L;

    /**
     * Used to avoid repeatedly restarting the timer from incidental inventory events.
     */
    private long lastInventorySignature = 0L;

    /**
     * This flag is set by block interactions (brew level changes etc).
     * It MUST be cleared after being handled, otherwise brewing can be constantly “reset”.
     */
    private boolean reValidateRecipe = false;

    private static final double TICKS_PER_SECOND = 20.0;

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
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                level.blockEvent(worldPosition, getBlockState().getBlock(), 0, 0);
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
        return !Arrays.asList(BLACKLISTED_INGREDIENTS).contains(stack.getItem())
                && !stack.isEmpty()
                && !(stack.getItem() instanceof SpawnEggItem);
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

    private boolean isInventoryEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    /** Single source of truth for “brewing time” in ticks (what recipes expect). */
    private long getElapsedBrewingTicksNow() {
        if (brewingStartMs <= 0L) return 0L;
        long now = System.currentTimeMillis();
        long elapsedMs = Math.max(0L, now - brewingStartMs);
        return (long) Math.floor((elapsedMs * TICKS_PER_SECOND) / 1000.0);
    }

    private long computeInventorySignature() {
        long sig = 1469598103934665603L; // FNV-1a offset basis
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack s = itemHandler.getStackInSlot(i);
            int itemHash = (s.isEmpty() ? 0 : Item.getId(s.getItem()));
            int count = s.getCount();
            long v = (((long) itemHash) << 32) ^ (count & 0xffffffffL);

            sig ^= v;
            sig *= 1099511628211L; // FNV prime
        }
        return sig;
    }

    public void serverTick(Level level) {
        if (level.isClientSide) return;

        updateHeatingState();

        if (isInventoryEmpty()) {
            resetBrewing();
            return;
        }

        // IMPORTANT: handle and then clear this flag
        if (reValidateRecipe) {
            reValidateRecipe = false;
            if (getBlockState().getValue(BrewingCauldronBlock.BREW_LEVEL) == 0) {
                resetBrewing();
                return;
            }
        }

        if (!isHeated()) {
            if (lockedRecipe != null || brewingStartMs != 0L) {
                lockedRecipe = null;
                brewingStartMs = 0L;
                syncedBrewingTicks = 0;
                lastInventorySignature = 0L;
                setCurrentColor(DEFAULT_COLOR);
                if (syncedBrewColor != DEFAULT_COLOR) syncedBrewColor = DEFAULT_COLOR;
                setChanged();
            }
            return;
        }

        // Sync derived ticks for client particle effects + clock parity
        int derivedTicks = (int) Math.min(Integer.MAX_VALUE, getElapsedBrewingTicksNow());
        if (syncedBrewingTicks != derivedTicks) {
            syncedBrewingTicks = derivedTicks;
            setChanged();
        }

        if (inventoryChanged) {
            inventoryChanged = false;

            long sig = computeInventorySignature();
            if (sig != lastInventorySignature) {
                lastInventorySignature = sig;

                SimpleContainer container = new SimpleContainer(itemHandler.getSlots());
                for (int i = 0; i < itemHandler.getSlots(); i++) {
                    container.setItem(i, itemHandler.getStackInSlot(i));
                }

                Optional<BrewingRecipe> match = level.getRecipeManager()
                        .getAllRecipesFor(ModRecipes.BREWING_RECIPE_TYPE).stream()
                        .filter(r -> r.matches(container, level))
                        .findFirst();

                if (match.isPresent()) {
                    BrewingRecipe newRecipe = match.get();
                    boolean recipeChanged = (lockedRecipe == null) || !lockedRecipe.getId().equals(newRecipe.getId());

                    lockedRecipe = newRecipe;
                    if (recipeChanged || brewingStartMs == 0L) {
                        brewingStartMs = System.currentTimeMillis();
                        syncedBrewingTicks = 0;
                    }

                    BrewType bt = BrewType.getBrewTypeFromId(lockedRecipe.getBrewTypeId());
                    int target = (bt != null) ? bt.tintColor() : DEFAULT_COLOR;
                    if (syncedBrewColor != target) syncedBrewColor = target;

                    setChanged();
                } else {
                    lockedRecipe = null;
                    brewingStartMs = 0L;
                    syncedBrewingTicks = 0;
                    setCurrentColor(DEFAULT_COLOR);
                    if (syncedBrewColor != DEFAULT_COLOR) syncedBrewColor = DEFAULT_COLOR;
                    setChanged();
                    return;
                }
            }
        }

        if (lockedRecipe != null && brewingStartMs > 0L) {
            BrewType brewType = BrewType.getBrewTypeFromId(lockedRecipe.getBrewTypeId());

            long elapsedTicks = getElapsedBrewingTicksNow();
            long optimalTicks = Math.max(0L, lockedRecipe.getOptimalBrewingTime());
            double maxTicks = optimalTicks * (1.0 + lockedRecipe.getMaxBrewingTimeError());

            int prevColor = currentColor;

            if (optimalTicks <= 0L) {
                setCurrentColor((brewType != null) ? brewType.tintColor() : DEFAULT_COLOR);
            } else if (elapsedTicks <= optimalTicks) {
                float progress = (float) ((double) elapsedTicks / (double) optimalTicks);
                progress = Math.max(0f, Math.min(1f, progress));
                if (brewType != null) {
                    setCurrentColor(UtilMath.expInterpolateColor(DEFAULT_COLOR, brewType.tintColor(), progress));
                } else {
                    setCurrentColor(DEFAULT_COLOR);
                }
            } else {
                if (elapsedTicks <= maxTicks && maxTicks > optimalTicks) {
                    float revertProgress = (float) ((elapsedTicks - optimalTicks) / (maxTicks - optimalTicks));
                    revertProgress = Math.max(0f, Math.min(1f, revertProgress));
                    if (brewType != null) {
                        setCurrentColor(UtilMath.expInterpolateColor(brewType.tintColor(), DEFAULT_FAILED_COLOR, revertProgress));
                    } else {
                        setCurrentColor(DEFAULT_COLOR);
                    }
                } else {
                    setCurrentColor(DEFAULT_FAILED_COLOR);
                }
            }

            if (currentColor != prevColor) setChanged();

            int targetBrewColor = (brewType != null) ? brewType.tintColor() : DEFAULT_COLOR;
            if (syncedBrewColor != targetBrewColor) {
                syncedBrewColor = targetBrewColor;
                setChanged();
            }
        } else {
            if (syncedBrewColor != DEFAULT_COLOR) {
                syncedBrewColor = DEFAULT_COLOR;
                setChanged();
            }
        }
    }

    public void clientTick(Level level) {
        if (level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            if (syncedBrewingTicks > 0 && level.getGameTime() % 5 == 0) {
                double x = worldPosition.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6;
                double y = worldPosition.getY() + 1.2;
                double z = worldPosition.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6;
                float r = ((syncedBrewColor >> 16) & 0xFF) / 255.0f;
                float g = ((syncedBrewColor >> 8) & 0xFF) / 255.0f;
                float b = (syncedBrewColor & 0xFF) / 255.0f;
                if (r == 0.0f && g == 0.0f && b == 0.0f) {
                    r = g = b = 0.001f;
                }
                level.addParticle(new DustParticleOptions(new Vector3f(r, g, b), 1.0f), x, y, z, 0, 0.2, 0);
            }
        }
    }

    public BrewingRecipe getLockedRecipe() {
        return lockedRecipe;
    }

    public int getBrewingTicks() {
        return (int) Math.min(Integer.MAX_VALUE, getElapsedBrewingTicksNow());
    }

    public void resetBrewing() {
        setCurrentColor(DEFAULT_COLOR);
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        setReValidateRecipe();
        lockedRecipe = null;
        brewingStartMs = 0L;
        syncedBrewingTicks = 0;
        lastInventorySignature = 0L;
        if (syncedBrewColor != DEFAULT_COLOR) syncedBrewColor = DEFAULT_COLOR;
        setChanged();
    }

    public ItemStack getCurrentStateRecipeResult() {
        if (lockedRecipe == null) return BrewType.DEFAULT_POTION();
        BrewType baseResult = BrewType.getBrewTypeFromId(lockedRecipe.getBrewTypeId());
        if (baseResult == null) return BrewType.DEFAULT_POTION();

        long elapsedTicks = getElapsedBrewingTicksNow();

        // This must be ticks; BrewType.toItem compares to recipe optimal time in ticks.
        ItemStack preliminary = baseResult.toItem(lockedRecipe, elapsedTicks, itemHandler);

        if (!lockedRecipe.requiresDistilling() && !lockedRecipe.requiresAging()) {
            CompoundTag tag = preliminary.getTag();
            if (tag == null || !tag.contains("purity")) {
                return preliminary;
            }
            int purity = tag.getInt("purity");
            return BrewType.buildFinalBrew(lockedRecipe, purity, level);
        }

        return preliminary;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        if (tag.contains("CurrentColor")) this.currentColor = tag.getInt("CurrentColor");
        if (tag.contains("BrewingTicks")) this.syncedBrewingTicks = tag.getInt("BrewingTicks");
        if (tag.contains("CurrentWaterColor")) this.syncedCurrentColor = tag.getInt("CurrentWaterColor");
        if (tag.contains("BrewTypeColor")) this.syncedBrewColor = tag.getInt("BrewTypeColor");
        if (tag.contains("BrewingStartMs")) this.brewingStartMs = tag.getLong("BrewingStartMs");
        if (tag.contains("LastInventorySignature")) this.lastInventorySignature = tag.getLong("LastInventorySignature");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putInt("CurrentColor", currentColor);
        tag.putInt("BrewingTicks", getBrewingTicks());
        tag.putInt("CurrentWaterColor", currentColor);
        tag.putInt("BrewTypeColor", syncedBrewColor);
        tag.putLong("BrewingStartMs", brewingStartMs);
        tag.putLong("LastInventorySignature", lastInventorySignature);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("CurrentColor", currentColor);
        tag.putInt("BrewingTicks", getBrewingTicks());
        tag.putInt("CurrentWaterColor", currentColor);
        tag.putInt("BrewTypeColor", syncedBrewColor);
        tag.putLong("BrewingStartMs", brewingStartMs);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (tag.contains("CurrentColor")) this.currentColor = tag.getInt("CurrentColor");
        if (tag.contains("BrewingTicks")) this.syncedBrewingTicks = tag.getInt("BrewingTicks");
        if (tag.contains("CurrentWaterColor")) this.syncedCurrentColor = tag.getInt("CurrentWaterColor");
        if (tag.contains("BrewTypeColor")) this.syncedBrewColor = tag.getInt("BrewTypeColor");
        if (tag.contains("BrewingStartMs")) this.brewingStartMs = tag.getLong("BrewingStartMs");
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
