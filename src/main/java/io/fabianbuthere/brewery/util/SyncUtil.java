package io.fabianbuthere.brewery.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Small utility to standardize safe server->client sync for block entities.
 * Only sends a block update when running on the logical server and level is present.
 */
public final class SyncUtil {
    private SyncUtil() {}

    public static void syncToClient(BlockEntity be) {
        if (be == null) return;
        Level level = be.getLevel();
        if (level == null || level.isClientSide) return;

        BlockPos pos = be.getBlockPos();
        BlockState state = be.getBlockState();
        // Flag 3: clients get the update, neighbors are notified and re-rendering is triggered.
        level.sendBlockUpdated(pos, state, state, 3);
    }
}