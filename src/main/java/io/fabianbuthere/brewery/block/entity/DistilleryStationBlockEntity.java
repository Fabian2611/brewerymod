package io.fabianbuthere.brewery.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class DistilleryStationBlockEntity extends BlockEntity {
    public DistilleryStationBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    public DistilleryStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISTILLERY_STATION.get(), pos, state);
    }
}
