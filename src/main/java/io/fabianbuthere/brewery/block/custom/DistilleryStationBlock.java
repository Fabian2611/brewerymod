package io.fabianbuthere.brewery.block.custom;

import io.fabianbuthere.brewery.block.entity.DistilleryStationBlockEntity;
import io.fabianbuthere.brewery.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DistilleryStationBlock extends BaseEntityBlock {
    public DistilleryStationBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return ModBlockEntities.DISTILLERY_STATION.get().create(pPos, pState);
    }
}
