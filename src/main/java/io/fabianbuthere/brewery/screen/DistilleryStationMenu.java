package io.fabianbuthere.brewery.screen;

import io.fabianbuthere.brewery.block.ModBlocks;
import io.fabianbuthere.brewery.block.entity.DistilleryStationBlockEntity;
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

public class DistilleryStationMenu extends AbstractContainerMenu {
    public final DistilleryStationBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public static final int INPUT_SLOT_IDX = 0;
    public static final int FILTER_SLOT_IDX = 1;
    public static final int OUTPUT_SLOT_IDX = 2;

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_ROW_COUNT * PLAYER_INVENTORY_COLUMN_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int TE_INVENTORY_SLOT_COUNT = 3;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    public static final Item[] ALLOWED_FILTER_ITEMS = new Item[] {Items.GLOWSTONE_DUST, Items.REDSTONE, Items.GUNPOWDER};

    public DistilleryStationMenu(int pContainerId, Inventory playerInv, BlockEntity blockEntity, ContainerData data) {
        super(ModMenus.DISTILLERY_STATION_MENU.get(), pContainerId);
        checkContainerSize(playerInv, 3);
        this.blockEntity = (DistilleryStationBlockEntity) blockEntity; // I did it this way for a reason, don't change it. What is that reason you ask? I don't know, but it was important at the time.
        this.level = playerInv.player.level();
        this.data = data;

        addPlayerInventory(playerInv);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
            this.addSlot(new SlotItemHandler(iItemHandler, INPUT_SLOT_IDX, 80, 12));
            this.addSlot(new FilterSlot(iItemHandler, FILTER_SLOT_IDX, 57, 35, ALLOWED_FILTER_ITEMS));
            this.addSlot(new SlotItemHandler(iItemHandler, OUTPUT_SLOT_IDX, 80, 58));
        });

        addDataSlots(data);
    }

    public boolean isCrafting() {
        return this.data.get(0) > 0;
    }

    public int getScaledProgress() {
        int progress = this.data.get(0);
        int maxProgress = this.data.get(1);
        if (maxProgress == 0) return 0;
        return (int) ((float) progress / maxProgress * 24);
    }

    public DistilleryStationMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(3));
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
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), pPlayer, ModBlocks.DISTILLERY_STATION.get());
    }

    public void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < PLAYER_INVENTORY_ROW_COUNT; i++) {
            for (int j = 0; j < PLAYER_INVENTORY_COLUMN_COUNT; j++) {
                int slotIndex = j + i * PLAYER_INVENTORY_COLUMN_COUNT + HOTBAR_SLOT_COUNT;
                this.addSlot(new Slot(playerInventory, slotIndex, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int i = 0; i < HOTBAR_SLOT_COUNT; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

}
