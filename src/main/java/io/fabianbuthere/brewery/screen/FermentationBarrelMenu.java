package io.fabianbuthere.brewery.screen;

import io.fabianbuthere.brewery.block.ModBlocks;
import io.fabianbuthere.brewery.block.entity.FermentationBarrelBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

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
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        Slot sourceSlot = slots.get(pIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        if (pIndex < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // Moving from TE inventory to player inventory
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
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
        sourceSlot.onTake(pPlayer, sourceStack);
        return copyStack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), pPlayer, ModBlocks.FERMENTATION_BARREL.get());
    }
}
