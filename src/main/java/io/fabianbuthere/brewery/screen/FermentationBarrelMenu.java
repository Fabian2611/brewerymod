package io.fabianbuthere.brewery.screen;

import io.fabianbuthere.brewery.block.ModBlocks;
import io.fabianbuthere.brewery.block.custom.FermentationBarrelBlock;
import io.fabianbuthere.brewery.block.entity.FermentationBarrelBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.NotNull;

public class FermentationBarrelMenu extends AbstractContainerMenu {
    public final FermentationBarrelBlockEntity blockEntity;
    private final Level level;

    public static final Item[] ALLOWED_INPUT_ITEMS = new Item[]{Items.POTION};

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_ROW_COUNT * PLAYER_INVENTORY_COLUMN_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int TE_INVENTORY_SLOT_COUNT = 9;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    public FermentationBarrelMenu(int pContainerId, Inventory pPlayerInventory, BlockEntity blockEntity, ContainerData data) {
        super(ModMenus.FERMENTATION_BARREL_MENU.get(), pContainerId);
        checkContainerSize(pPlayerInventory, 9);
        this.blockEntity = (FermentationBarrelBlockEntity) blockEntity;
        this.level = pPlayerInventory.player.level();

        addPlayerInventory(pPlayerInventory);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
            for (int i = 0; i < 9; i++) {
                this.addSlot(new FilterSlot(iItemHandler, i, 8 + i * 18, 18, ALLOWED_INPUT_ITEMS));
            }
        });

        addDataSlots(data);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < PLAYER_INVENTORY_ROW_COUNT; i++) {
            for (int j = 0; j < PLAYER_INVENTORY_COLUMN_COUNT; j++) {
                int slotIndex = j + i * PLAYER_INVENTORY_COLUMN_COUNT + HOTBAR_SLOT_COUNT;
                this.addSlot(new Slot(playerInventory, slotIndex, 8 + j * 18, 50 + i * 18));
            }
        }

        for (int i = 0; i < HOTBAR_SLOT_COUNT; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 108));
        }
    }

    public FermentationBarrelMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(9));
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        Slot sourceSlot = slots.get(pIndex);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        // Moving from TE inventory to player inventory
        if (pIndex >= TE_INVENTORY_FIRST_SLOT_INDEX && pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            int teSlot = pIndex - TE_INVENTORY_FIRST_SLOT_INDEX;
            this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
                ItemStack extracted = iItemHandler.extractItem(teSlot, sourceStack.getCount(), false);
                if (!extracted.isEmpty()) {
                    if (!moveItemStackTo(extracted, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                        iItemHandler.insertItem(teSlot, extracted, false);
                    }
                }
            });
            sourceSlot.set(ItemStack.EMPTY);
            return copyStack;
        }

        // Moving from player inventory to TE inventory (normal insert logic)
        if (pIndex < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        return copyStack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        // Accept any fermentation barrel variant
        return blockEntity != null && ModBlocks.FERMENTATION_BARRELS.values().stream()
            .anyMatch(barrel -> blockEntity.getBlockState().is(barrel.get()));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            // Check if no more players are viewing the menu
            if (blockEntity != null && blockEntity.getLevel() != null) {
                // Count viewers for this block entity
                int viewers = 0;
                for (Player p : blockEntity.getLevel().players()) {
                    if (p.containerMenu instanceof FermentationBarrelMenu menu && menu.blockEntity.getBlockPos().equals(blockEntity.getBlockPos())) {
                        viewers++;
                    }
                }
                if (viewers <= 1) { // This player is closing, so if only 1, it's the last
                    FermentationBarrelBlock.closeBarrel(blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState());
                }
            }
        }
    }
}
