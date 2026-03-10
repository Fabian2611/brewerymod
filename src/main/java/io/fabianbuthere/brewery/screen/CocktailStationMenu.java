package io.fabianbuthere.brewery.screen;

import io.fabianbuthere.brewery.block.ModBlocks;
import io.fabianbuthere.brewery.block.entity.CocktailStationBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class CocktailStationMenu extends AbstractContainerMenu {
    public final CocktailStationBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_ROW_COUNT * PLAYER_INVENTORY_COLUMN_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int TE_INVENTORY_SLOT_COUNT = CocktailStationBlockEntity.TOTAL_SLOTS;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    public CocktailStationMenu(int pContainerId, Inventory playerInv, BlockEntity blockEntity, ContainerData data) {
        super(ModMenus.COCKTAIL_STATION_MENU.get(), pContainerId);
        this.blockEntity = (CocktailStationBlockEntity) blockEntity;
        this.level = playerInv.player.level();
        this.data = data;

        addPlayerInventory(playerInv);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
            // 4 brew input slots in a 2×2 grid
            this.addSlot(new SlotItemHandler(iItemHandler, 0, 44, 17));  // top-left
            this.addSlot(new SlotItemHandler(iItemHandler, 1, 62, 17));  // top-right
            this.addSlot(new SlotItemHandler(iItemHandler, 2, 44, 35));  // bottom-left
            this.addSlot(new SlotItemHandler(iItemHandler, 3, 62, 35));  // bottom-right
            // Output slot
            this.addSlot(new SlotItemHandler(iItemHandler, CocktailStationBlockEntity.OUTPUT_SLOT, 116, 26));
        });

        addDataSlots(data);
    }

    public CocktailStationMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(2));
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

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player pPlayer, int pIndex) {
        Slot sourceSlot = slots.get(pIndex);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        if (pIndex < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX,
                    TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX,
                    VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
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
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                pPlayer, ModBlocks.COCKTAIL_STATION.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
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
