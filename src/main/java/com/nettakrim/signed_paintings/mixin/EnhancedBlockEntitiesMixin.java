package com.nettakrim.signed_paintings.mixin;

import com.nettakrim.signed_paintings.access.SignBlockEntityRendererAccessor;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "foundationgames.enhancedblockentities.client.render.entity.SignBlockEntityRendererOverride")
public class EnhancedBlockEntitiesMixin {
    @Inject(at = @At(value = "HEAD"), method = "render", cancellable = true)
    private void enhancedRender(BlockEntityRenderer<BlockEntity> renderer, BlockEntity blockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, CallbackInfo ci) {
        if (((SignBlockEntityRendererAccessor)renderer).signedPaintings$enhancedRender(blockEntity, tickDelta, matrices, vertexConsumers, light, overlay)) {
            ci.cancel();
        }
    }
}
