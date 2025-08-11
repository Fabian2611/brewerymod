package io.fabianbuthere.brewery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.fabianbuthere.brewery.block.custom.BrewShelfBlock;
import io.fabianbuthere.brewery.block.entity.BrewShelfBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class BrewShelfRenderer implements BlockEntityRenderer<BrewShelfBlockEntity> {

    public BrewShelfRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(BrewShelfBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack bottomLeft = be.getItem(0);
        ItemStack bottomRight = be.getItem(1);
        ItemStack topLeft = be.getItem(2);
        ItemStack topRight = be.getItem(3);

        Direction facing = be.getBlockState().getValue(BrewShelfBlock.FACING);

        // Render parameters
        final float scale = 0.38f;
        final float epsilon = 0.001f;

        // Base positions for EAST-facing model (front +X).
        // Move bottles closer to the backboard: backboard is at x 0..2, shelves start at x=2.
        // Place bottles just in front of the backboard.
        float xDepth = 4.0f / 16.0f;

        // Columns across Z within 0..16
        float zLeft  = 4.0f / 16.0f;
        float zRight = 12.0f / 16.0f;

        // Shelf tops at y=2 and y=10. Translate by half the item height so bottoms sit on the shelf.
        float yBottom = (2.0f / 16.0f) + epsilon + (scale * 0.5f);
        float yTop    = (10.0f / 16.0f) + epsilon + (scale * 0.5f);

        renderBottle(bottomLeft, poseStack, bufferSource, packedLight, packedOverlay, xDepth, yBottom, zLeft, facing, scale);
        renderBottle(bottomRight, poseStack, bufferSource, packedLight, packedOverlay, xDepth, yBottom, zRight, facing, scale);
        renderBottle(topLeft, poseStack, bufferSource, packedLight, packedOverlay, xDepth, yTop, zLeft, facing, scale);
        renderBottle(topRight, poseStack, bufferSource, packedLight, packedOverlay, xDepth, yTop, zRight, facing, scale);
    }

    private void renderBottle(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay,
                              float x, float y, float z, Direction facing, float scale) {
        if (stack == null || stack.isEmpty()) return;

        poseStack.pushPose();

        // Rotate the position from EAST basis to the block's FACING
        int turnsCW = turnsFromEast(facing); // EAST=0, SOUTH=1, WEST=2, NORTH=3
        float[] rotated = rotateAroundCenter(x, z, turnsCW);
        float rx = rotated[0];
        float rz = rotated[1];

        // Translate to the final spot
        poseStack.translate(rx, y, rz);

        // Make bottle face outward toward the player and compensate for the 90° offset observed with FIXED transform.
        // This adds +90° to the usual outward yaw.
        float yaw = switch (facing) {
            case NORTH -> 0f;
            case EAST  -> 90f;
            case SOUTH -> 180f;
            case WEST  -> 270f;
            default    -> 0f;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));

        // Scale to fit shelf
        poseStack.scale(scale, scale, scale);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                light,
                overlay,
                poseStack,
                buffer,
                Minecraft.getInstance().level,
                0
        );

        poseStack.popPose();
    }

    // Rotate a point around the block center (0.5, 0.5) by 90-degree clockwise steps.
    // One CW step: (x, z) -> (1 - z, x).
    private static float[] rotateAroundCenter(float x, float z, int turnsCW) {
        int t = ((turnsCW % 4) + 4) % 4;
        float rx = x, rz = z;
        for (int i = 0; i < t; i++) {
            float nx = 1.0f - rz;
            float nz = rx;
            rx = nx;
            rz = nz;
        }
        return new float[]{rx, rz};
    }

    private static int turnsFromEast(Direction facing) {
        return switch (facing) {
            case EAST -> 0;
            case SOUTH -> 1;
            case WEST -> 2;
            case NORTH -> 3;
            default -> 0;
        };
    }
}