package io.fabianbuthere.brewery.block.custom;

import io.fabianbuthere.brewery.block.entity.FermentationBarrelBlockEntity;
import io.fabianbuthere.brewery.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class FermentationBarrelBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(2, 0, 0, 14, 14, 16);
    public static final EnumProperty<WoodType> WOOD_TYPE = EnumProperty.create("wood_type", WoodType.class);
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    private final WoodType woodType;

    public FermentationBarrelBlock(Properties pProperties, WoodType woodType) {
        super(pProperties);
        this.woodType = woodType;
        this.registerDefaultState(this.getStateDefinition().any().setValue(WOOD_TYPE, woodType).setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(WOOD_TYPE, OPEN);
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
        // Set open to false when block is removed (GUI closed)
        if (pLevel != null && !pLevel.isClientSide() && pState.getValue(OPEN)) {
            pLevel.setBlock(pPos, pState.setValue(OPEN, false), 3);
        }
        if (!pState.is(pNewState.getBlock())) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof net.minecraftforge.items.IItemHandler handler) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Block.popResource(pLevel, pPos, stack);
                    }
                }
            }
            super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
        }
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FermentationBarrelBlockEntity) {
                if (!state.getValue(OPEN)) {
                    level.setBlock(pos, state.setValue(OPEN, true), 3);
                }
                NetworkHooks.openScreen((ServerPlayer) player, (FermentationBarrelBlockEntity) be, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public static void closeBarrel(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide() && state.getBlock() instanceof FermentationBarrelBlock && state.getValue(OPEN)) {
            level.setBlock(pos, state.setValue(OPEN, false), 3);
        }
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if (pLevel.isClientSide()) return null;
        WoodType type = pState.getValue(WOOD_TYPE);
        BlockEntityType<?> correctType = ModBlockEntities.FERMENTATION_BARRELS.get(type).get();
        return createTickerHelper(pBlockEntityType, correctType, (lvl, pos, state, be) -> {
            if (be instanceof FermentationBarrelBlockEntity barrel) {
                barrel.tick(lvl, pos, state);
            }
        });
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new FermentationBarrelBlockEntity(pPos, pState);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(WOOD_TYPE, this.woodType);
    }

    private Block woodTypeToPlankBlock(WoodType type) {
        return switch (type) {
            case ACACIA -> Blocks.ACACIA_PLANKS;
            case BAMBOO -> Blocks.BAMBOO_PLANKS;
            case BIRCH -> Blocks.BIRCH_PLANKS;
            case CHERRY -> Blocks.CHERRY_PLANKS;
            case DARK_OAK -> Blocks.DARK_OAK_PLANKS;
            case JUNGLE -> Blocks.JUNGLE_PLANKS;
            case MANGROVE -> Blocks.MANGROVE_PLANKS;
            case OAK -> Blocks.OAK_PLANKS;
            case CRIMSON -> Blocks.CRIMSON_PLANKS;
            case WARPED -> Blocks.WARPED_PLANKS;
            case SPRUCE -> Blocks.SPRUCE_PLANKS;
        };
    }

    @Override
    public boolean propagatesSkylightDown(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return true;
    }

    @Override
    public int getLightBlock(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 0;
    }

    @Override
    public boolean isCollisionShapeFullBlock(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return false;
    }

    @Override
    public boolean isOcclusionShapeFullBlock(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return false;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(this);
    }
}
