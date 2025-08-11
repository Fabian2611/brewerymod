package io.fabianbuthere.brewery.block.custom;

import io.fabianbuthere.brewery.block.entity.BrewShelfBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class BrewShelfBlock extends BaseEntityBlock {
    // Base model: front is +X (EAST). Depth is 7px.
    private static final VoxelShape SHAPE_EAST  = Block.box(0.0, 0.0, 0.0, 7.0, 16.0, 16.0); // depth on X
    private static final VoxelShape SHAPE_SOUTH = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 7.0); // depth on Z
    private static final VoxelShape SHAPE_WEST  = Block.box(9.0, 0.0, 0.0, 16.0, 16.0, 16.0); // depth on X (from right)
    private static final VoxelShape SHAPE_NORTH = Block.box(0.0, 0.0, 9.0, 16.0, 16.0, 16.0); // depth on Z (from back)

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public BrewShelfBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Face the player (front points toward the player)
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return switch (pState.getValue(FACING)) {
            case EAST  -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            case WEST  -> SHAPE_WEST;
            case NORTH -> SHAPE_NORTH;
            default    -> SHAPE_EAST;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BrewShelfBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = pLevel.getBlockEntity(pPos);
        if (!(be instanceof BrewShelfBlockEntity shelf)) return InteractionResult.PASS;

        Direction facing = pState.getValue(FACING);

        // Only interact when clicking the front face to avoid weird side interactions
        if (pHit.getDirection() != facing) {
            return InteractionResult.PASS;
        }

        // Local hit in [0,1)
        double hx = pHit.getLocation().x - pPos.getX();
        double hy = pHit.getLocation().y - pPos.getY();
        double hz = pHit.getLocation().z - pPos.getZ();
        float x = (float) Math.max(0.0, Math.min(0.999, hx));
        float y = (float) Math.max(0.0, Math.min(0.999, hy));
        float z = (float) Math.max(0.0, Math.min(0.999, hz));

        boolean top = y >= 0.5f;

        // Determine left/right relative to the block front using a dot against the "left" axis
        Direction leftDir = facing.getCounterClockWise();
        int lx = leftDir.getStepX();
        int lz = leftDir.getStepZ();
        float cx = x - 0.5f;
        float cz = z - 0.5f;
        boolean left = (cx * lx + cz * lz) >= 0.0f;

        int slot = computeSlot(left, top);

        ItemStack inHand = pPlayer.getItemInHand(pHand);
        ItemStack slotStack = shelf.getItem(slot);

        // Return existing item if slot is occupied
        if (!slotStack.isEmpty()) {
            ItemStack toGive = slotStack.copy();
            shelf.setItem(slot, ItemStack.EMPTY);
            if (!pPlayer.addItem(toGive)) {
                pPlayer.drop(toGive, false);
            }
            return InteractionResult.CONSUME;
        }

        // Insert one potion bottle
        if (isPotion(inHand)) {
            ItemStack one = inHand.copy();
            one.setCount(1);
            shelf.setItem(slot, one);
            inHand.shrink(1);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    private static int computeSlot(boolean left, boolean top) {
        // 0: bottom-left, 1: bottom-right, 2: top-left, 3: top-right
        if (top) {
            return left ? 2 : 3;
        } else {
            return left ? 0 : 1;
        }
    }

    private static boolean isPotion(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }

    @Override
    public void destroy(LevelAccessor pLevel, BlockPos pPos, BlockState pState) {
        if (pLevel instanceof Level level) {
            BlockEntity be = level.getBlockEntity(pPos);
            if (be instanceof BrewShelfBlockEntity shelf) {
                Containers.dropContents(level, pPos, shelf.getDropsContainer());
            }
        }
        super.destroy(pLevel, pPos, pState);
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pState.is(pNewState.getBlock())) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if (be instanceof BrewShelfBlockEntity shelf) {
                Containers.dropContents(pLevel, pPos, shelf.getDropsContainer());
            }
            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
        } else {
            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
        }
    }
}