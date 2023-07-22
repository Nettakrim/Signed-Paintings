package com.nettakrim.mixin;

import com.nettakrim.PaintingInfo;
import com.nettakrim.SignedPaintingsClient;
import com.nettakrim.access.SignBlockEntityAccessor;
import net.minecraft.block.*;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignBlockEntityRenderer.class)
public abstract class SignBlockEntityRendererMixin {
    @Inject(
            at = @At(
                    value = "HEAD",
                    target = "Lnet/minecraft/client/render/block/entity/SignBlockEntityRenderer;renderSign(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/block/WoodType;Lnet/minecraft/client/model/Model;)V"
            ),
            method = "render(Lnet/minecraft/block/entity/SignBlockEntity;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/block/BlockState;Lnet/minecraft/block/AbstractSignBlock;Lnet/minecraft/block/WoodType;Lnet/minecraft/client/model/Model;)V",
            cancellable = true
    )
    private void onRender(SignBlockEntity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, BlockState state, AbstractSignBlock block, WoodType woodType, Model model, CallbackInfo ci) {
        boolean success = false;
        SignBlockEntityAccessor accessor = (SignBlockEntityAccessor)entity;
        success |= renderPaintingInfo(accessor.signedPaintings$getFrontPaintingInfo(), matrices, vertexConsumers, model, light, block, state);
        success |= renderPaintingInfo(accessor.signedPaintings$getBackPaintingInfo(), matrices, vertexConsumers, model, light, block, state);
        if (success) ci.cancel();
    }

    @Unique
    private boolean renderPaintingInfo(PaintingInfo info, MatrixStack matrices, VertexConsumerProvider vertexConsumers, Model model, int light, AbstractSignBlock block, BlockState state) {
        if (info != null && info.isReady()) {
            SignedPaintingsClient.paintingRenderer.renderPainting(matrices, vertexConsumers, model, info, light, -block.getRotationDegrees(state));
            return true;
        }
        return false;
    }
}
