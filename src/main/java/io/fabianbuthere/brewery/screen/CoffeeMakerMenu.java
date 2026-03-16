package io.fabianbuthere.brewery.screen;

import io.fabianbuthere.brewery.block.ModBlocks;
import io.fabianbuthere.brewery.block.entity.CoffeeMakerBlockEntity;
import io.fabianbuthere.brewery.item.ModItems;
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
import org.jetbrains.annotations.NotNull;

public class CoffeeMakerMenu extends AbstractContainerMenu {
    public final CoffeeMakerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_ROW_COUNT * PLAYER_INVENTORY_COLUMN_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;

    private static final int TE_INVENTORY_SLOT_COUNT = CoffeeMakerBlockEntity.TOTAL_SLOTS;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    private static final Item[] ALLOWED_BREW_ITEMS = new Item[]{Items.POTION};

    private static final Item[] ALLOWED_BEAN_ITEMS = new Item[]{ModItems.ROASTED_COFFEE_BEAN.get()};

    public CoffeeMakerMenu(int pContainerId, Inventory playerInv, BlockEntity blockEntity, ContainerData data) {
        super(ModMenus.COFFEE_MAKER_MENU.get(), pContainerId);
        this.blockEntity = (CoffeeMakerBlockEntity) blockEntity;
        this.level = playerInv.player.level();
        this.data = data;

        addPlayerInventory(playerInv);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
            // Left side brew slots (0-2) - only potions
            this.addSlot(new FilterSlot(iItemHandler, CoffeeMakerBlockEntity.BREW_SLOTS_START, 15, 8, ALLOWED_BREW_ITEMS));
            this.addSlot(new FilterSlot(iItemHandler, CoffeeMakerBlockEntity.BREW_SLOTS_START + 1, 35, 8, ALLOWED_BREW_ITEMS));
            this.addSlot(new FilterSlot(iItemHandler, CoffeeMakerBlockEntity.BREW_SLOTS_START + 2, 55, 8, ALLOWED_BREW_ITEMS));

            // Left side extra slots (3-4) - any items
            this.addSlot(new SlotItemHandler(iItemHandler, CoffeeMakerBlockEntity.EXTRA_SLOTS_START, 25, 27));
            this.addSlot(new SlotItemHandler(iItemHandler, CoffeeMakerBlockEntity.EXTRA_SLOTS_START + 1, 45, 27));

            // Left output (5) - output-only
            this.addSlot(new OutputSlot(iItemHandler, CoffeeMakerBlockEntity.LEFT_OUTPUT_SLOT, 35, 55));

            // Right input (6) - only coffee bean
            this.addSlot(new FilterSlot(iItemHandler, CoffeeMakerBlockEntity.BEAN_INPUT_SLOT, 135, 9, ALLOWED_BEAN_ITEMS));

            // Right output (7) - output-only
            this.addSlot(new OutputSlot(iItemHandler, CoffeeMakerBlockEntity.BEAN_OUTPUT_SLOT, 135, 53));
        });

        addDataSlots(data);
    }

    public CoffeeMakerMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
    }

    public boolean isLeftCrafting() {
        return this.data.get(0) > 0;
    }

    public boolean isRightCrafting() {
        return this.data.get(2) > 0;
    }

    public int getLeftScaledProgress(int pixels) {
        int progress = this.data.get(0);
        int maxProgress = this.data.get(1);
        if (maxProgress == 0) return 0;
        return (int) ((float) progress / maxProgress * pixels);
    }

    public int getRightScaledProgress(int pixels) {
        int progress = this.data.get(2);
        int maxProgress = this.data.get(3);
        if (maxProgress == 0) return 0;
        return (int) ((float) progress / maxProgress * pixels);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player pPlayer, int pIndex) {
        Slot sourceSlot = slots.get(pIndex);
        if (!sourceSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        final int teStart = TE_INVENTORY_FIRST_SLOT_INDEX;
        final int teEnd = TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT;

        final int leftBrewStart = teStart + CoffeeMakerBlockEntity.BREW_SLOTS_START;
        final int leftBrewEnd = leftBrewStart + CoffeeMakerBlockEntity.BREW_SLOT_COUNT;

        final int leftExtraStart = teStart + CoffeeMakerBlockEntity.EXTRA_SLOTS_START;
        final int leftExtraEnd = leftExtraStart + CoffeeMakerBlockEntity.EXTRA_SLOT_COUNT;

        final int rightInputStart = teStart + CoffeeMakerBlockEntity.BEAN_INPUT_SLOT;
        final int rightInputEnd = rightInputStart + 1;

        final int playerStart = VANILLA_FIRST_SLOT_INDEX;
        final int playerEnd = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

        if (pIndex >= playerStart && pIndex < playerEnd) {
            boolean moved = false;

            if (sourceStack.getItem() == Items.POTION) {
                moved = moveItemStackTo(sourceStack, leftBrewStart, leftBrewEnd, false)
                        || moveItemStackTo(sourceStack, leftExtraStart, leftExtraEnd, false);
            } else if (sourceStack.getItem() == ModItems.ROASTED_COFFEE_BEAN.get()) {
                moved = moveItemStackTo(sourceStack, rightInputStart, rightInputEnd, false);
            } else {
                moved = moveItemStackTo(sourceStack, leftExtraStart, leftExtraEnd, false);
            }

            if (!moved) return ItemStack.EMPTY;
        } else if (pIndex >= teStart && pIndex < teEnd) {
            if (!moveItemStackTo(sourceStack, playerStart, playerEnd, false)) {
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
                pPlayer, ModBlocks.COFFEE_MAKER.get());
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
