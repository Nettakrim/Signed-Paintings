package com.nettakrim.signed_paintings.mixin;

import com.nettakrim.signed_paintings.access.SignBlockEntityRendererAccessor;
import com.nettakrim.signed_paintings.rendering.PaintingInfo;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.access.SignBlockEntityAccessor;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(SignBlockEntityRenderer.class)
public abstract class SignBlockEntityRendererMixin implements SignBlockEntityRendererAccessor {
    @Shadow @Final private Map<WoodType, SignBlockEntityRenderer.SignModel> typeToModel;

    @Inject(
            at = @At(
                    value = "HEAD",
                    target = "Lnet/minecraft/client/render/block/entity/SignBlockEntityRenderer;renderSign(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/block/WoodType;Lnet/minecraft/client/model/Model;)V"
            ),
            method = "render(Lnet/minecraft/block/entity/SignBlockEntity;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/block/BlockState;Lnet/minecraft/block/AbstractSignBlock;Lnet/minecraft/block/WoodType;Lnet/minecraft/client/model/Model;)V",
            cancellable = true
    )
    private void onRender(SignBlockEntity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, BlockState state, AbstractSignBlock block, WoodType woodType, Model model, CallbackInfo ci) {
        if (!SignedPaintingsClient.renderSigns) return;
        boolean success = false;
        SignBlockEntityAccessor accessor = (SignBlockEntityAccessor)entity;
        accessor.signedPaintings$reloadIfNeeded();
        success |= renderPaintingInfo(accessor.signedPaintings$getFrontPaintingInfo(), matrices, vertexConsumers, model, light, block, state);
        success |= renderPaintingInfo(accessor.signedPaintings$getBackPaintingInfo(),  matrices, vertexConsumers, model, light, block, state);
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

    @Override
    public boolean signedPaintings$enhancedRender(BlockEntity signBlockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (!SignedPaintingsClient.renderSigns) return false;

        BlockState blockState = signBlockEntity.getCachedState();
        AbstractSignBlock block = (AbstractSignBlock)blockState.getBlock();
        WoodType woodType = AbstractSignBlock.getWoodType(block);
        SignBlockEntityRenderer.SignModel model = typeToModel.get(woodType);

        SignBlockEntityAccessor accessor = (SignBlockEntityAccessor)signBlockEntity;
        accessor.signedPaintings$reloadIfNeeded();
        boolean success = false;
        success |= renderPaintingInfo(accessor.signedPaintings$getFrontPaintingInfo(), matrices, vertexConsumers, model, light, block, blockState);
        success |= renderPaintingInfo(accessor.signedPaintings$getBackPaintingInfo(),  matrices, vertexConsumers, model, light, block, blockState);
        return success;
    }
}
