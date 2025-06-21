package io.fabianbuthere.brewery.block.custom;

import io.fabianbuthere.brewery.block.entity.BrewingCauldronBlockEntity;
import io.fabianbuthere.brewery.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class BrewingCauldronBlock extends BaseEntityBlock {
    private static final VoxelShape INSIDE = box(2.0D, 4.0D, 2.0D, 14.0D, 16.0D, 14.0D);
    protected static final VoxelShape SHAPE = Shapes.join(Shapes.block(), Shapes.or(box(0.0D, 0.0D, 4.0D, 16.0D, 3.0D, 12.0D), box(4.0D, 0.0D, 0.0D, 12.0D, 3.0D, 16.0D), box(2.0D, 0.0D, 2.0D, 14.0D, 3.0D, 14.0D), INSIDE), BooleanOp.ONLY_FIRST);
    public static final IntegerProperty BREW_LEVEL = IntegerProperty.create("brew_level", 0, 3);

    public BrewingCauldronBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BREW_LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(BREW_LEVEL);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BrewingCauldronBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        ItemStack held = pPlayer.getItemInHand(pHand);
        Item heldItem = held.getItem();
        int currentLevel = pState.getValue(BREW_LEVEL);

        if (heldItem == Items.BUCKET && currentLevel == 3) {
            if (!pLevel.isClientSide) {
                pLevel.setBlock(pPos, pState.setValue(BREW_LEVEL, 0), 3);
                if (!pPlayer.getAbilities().instabuild) held.shrink(1);
                if (!pPlayer.getInventory().add(new ItemStack(Items.WATER_BUCKET))) {
                    pPlayer.drop(new ItemStack(Items.WATER_BUCKET), false);
                }
                BrewingCauldronBlockEntity be = (BrewingCauldronBlockEntity) pLevel.getBlockEntity(pPos);
                if (be != null) be.setReValidateRecipe();
            }
            return InteractionResult.sidedSuccess(pLevel.isClientSide);
        }

        if (heldItem == Items.GLASS_BOTTLE && currentLevel > 0) {
            if (!pLevel.isClientSide) {
                BrewingCauldronBlockEntity be = (BrewingCauldronBlockEntity) pLevel.getBlockEntity(pPos);
                if (be != null) {
                    pLevel.setBlock(pPos, pState.setValue(BREW_LEVEL, currentLevel - 1), 3);
                    ItemStack result = be.getCurrentStateRecipeResult();
                    if (!pPlayer.getInventory().add(result)) {
                        pPlayer.drop(result, false);
                    }
                    if (!pPlayer.getAbilities().instabuild) held.shrink(1);
                    be.setReValidateRecipe();
                }
            }
            return InteractionResult.sidedSuccess(pLevel.isClientSide);
        }

        if (heldItem == Items.WATER_BUCKET && currentLevel < 3) {
            if (!pLevel.isClientSide) {
                pLevel.setBlock(pPos, pState.setValue(BREW_LEVEL, 3), 3);
                if (!pPlayer.getAbilities().instabuild) {
                    pPlayer.setItemInHand(pHand, new ItemStack(Items.BUCKET));
                }
                BrewingCauldronBlockEntity be = (BrewingCauldronBlockEntity) pLevel.getBlockEntity(pPos);
                if (be != null) be.resetBrewing();
                if (be != null) be.setReValidateRecipe();
            }
            return InteractionResult.sidedSuccess(pLevel.isClientSide);
        }

        if (heldItem == Items.POTION && held.getTag() != null && "minecraft:water".equals(held.getTag().getString("Potion")) && currentLevel < 3) {
            if (!pLevel.isClientSide) {
                int newLevel = Math.min(3, currentLevel + 1);
                pLevel.setBlock(pPos, pState.setValue(BREW_LEVEL, newLevel), 3);
                if (!pPlayer.getAbilities().instabuild) {
                    held.shrink(1);
                    pPlayer.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
                }
                BrewingCauldronBlockEntity be = (BrewingCauldronBlockEntity) pLevel.getBlockEntity(pPos);
                if (be != null) be.resetBrewing();
                if (be != null) be.setReValidateRecipe();
            }
            return InteractionResult.sidedSuccess(pLevel.isClientSide);
        }

        if (!pLevel.isClientSide) {
            BrewingCauldronBlockEntity be = (BrewingCauldronBlockEntity) pLevel.getBlockEntity(pPos);
            if (be != null) {
                if (!held.isEmpty() && be.canInsert(held)) {
                    ItemStack toInsert = held.copy();
                    ItemStack remainder = be.insertItemStack(toInsert);
                    int inserted = held.getCount() - remainder.getCount();
                    if (inserted > 0) {
                        held.shrink(inserted);
                        be.setReValidateRecipe();
                        return InteractionResult.CONSUME;
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.BREWING_CAULDRON.get()) {
            return (lvl, pos, st, be) -> {
                if (be instanceof BrewingCauldronBlockEntity cauldron) {
                    if (lvl.isClientSide) {
                        cauldron.clientTick(lvl);
                    } else {
                        cauldron.serverTick(lvl);
                    }
                }
            };
        }
        return null;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (fromPos.equals(pos.below())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BrewingCauldronBlockEntity cauldron) {
                cauldron.updateHeatingState();
            }
        }
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(oldState, level, pos, newState, isMoving);
        if (!level.isClientSide && oldState.hasProperty(BREW_LEVEL) && newState.hasProperty(BREW_LEVEL)) {
            int oldLevel = oldState.getValue(BREW_LEVEL);
            int newLevel = newState.getValue(BREW_LEVEL);
            if (oldLevel != newLevel) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof BrewingCauldronBlockEntity cauldron) {
                    cauldron.setReValidateRecipe();
                }
            }
        }
    }
}
