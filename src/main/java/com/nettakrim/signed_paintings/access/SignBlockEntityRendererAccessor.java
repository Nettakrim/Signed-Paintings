package com.nettakrim.signed_paintings.access;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface SignBlockEntityRendererAccessor {
    boolean signedPaintings$enhancedRender(BlockEntity blockEntity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay);
}
