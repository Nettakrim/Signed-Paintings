package com.nettakrim.signed_paintings.mixin;

import com.nettakrim.signed_paintings.access.SignBlockEntityRenderStateAccessor;
import com.nettakrim.signed_paintings.access.SignBlockEntityRendererAccessor;
import com.nettakrim.signed_paintings.rendering.PaintingInfo;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.access.SignBlockEntityAccessor;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.AbstractSignBlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.state.SignBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignBlockEntityRenderer.class)
public abstract class SignBlockEntityRendererMixin implements SignBlockEntityRendererAccessor, BlockEntityRenderer<SignBlockEntity, SignBlockEntityRenderState> {
    @Inject(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/block/entity/AbstractSignBlockEntityRenderer;renderSign(Lnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/block/WoodType;Lnet/minecraft/client/model/Model$SinglePartModel;Lnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V"
            ),
            method = "render(Lnet/minecraft/client/render/block/entity/state/SignBlockEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/AbstractSignBlock;Lnet/minecraft/block/WoodType;Lnet/minecraft/client/model/Model$SinglePartModel;Lnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V",
            cancellable = true
    )
    private void onRender(SignBlockEntityRenderState renderState, MatrixStack matrices, BlockState blockState, AbstractSignBlock block, WoodType woodType, Model.SinglePartModel model, ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay, OrderedRenderCommandQueue queue, CallbackInfo ci) {
        if (renderPaintings(renderState, matrices, block, queue)) {
            matrices.pop();
            ci.cancel();
        }
    }

    @Override
    public boolean signedPaintings$enhancedRender(BlockEntity signBlockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (!SignedPaintingsClient.renderSigns) return false;

        //return renderPaintings();
        return false;
    }

    @Unique
    private boolean renderPaintings(SignBlockEntityRenderState renderState, MatrixStack matrices, AbstractSignBlock block, OrderedRenderCommandQueue queue) {
        if (!SignedPaintingsClient.renderSigns) return false;

        boolean success = false;
        SignBlockEntityRenderStateAccessor accessor = (SignBlockEntityRenderStateAccessor)renderState;
        success |= renderPaintingInfo(accessor.signedPaintings$getFrontInfo(), queue, matrices, renderState, renderState.frontText);
        success |= renderPaintingInfo(accessor.signedPaintings$getBackInfo(), queue, matrices, renderState, renderState.backText);
        return success;
    }

    @Unique
    private boolean renderPaintingInfo(PaintingInfo info, OrderedRenderCommandQueue queue, MatrixStack matrices, SignBlockEntityRenderState state, SignText text) {
        if (info != null && info.isReady()) {
            SignedPaintingsClient.paintingRenderer.renderOrQueuePainting(matrices, queue, info, text != null && text.isGlowing() ? -1 : state.lightmapCoordinates);
            return true;
        }
        return false;
    }

    public boolean isInRenderDistance(SignBlockEntity blockEntity, Vec3d pos) {
        return (hasPainting((SignBlockEntityAccessor)blockEntity) && SignedPaintingsClient.reduceCulling) || BlockEntityRenderer.super.isInRenderDistance(blockEntity, pos);
    }

    @Unique
    private boolean hasPainting(SignBlockEntityAccessor accessor) {
        if (!SignedPaintingsClient.renderSigns) return false;
        PaintingInfo paintingInfo = accessor.signedPaintings$getFrontPaintingInfo();
        if (paintingInfo != null && paintingInfo.isReady()) return true;
        paintingInfo = accessor.signedPaintings$getBackPaintingInfo();
        return paintingInfo != null && paintingInfo.isReady();
    }

    @Inject(at = @At("TAIL"), method = "updateRenderState(Lnet/minecraft/block/entity/SignBlockEntity;Lnet/minecraft/client/render/block/entity/state/SignBlockEntityRenderState;FLnet/minecraft/util/math/Vec3d;Lnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V")
    private void updateRenderState(SignBlockEntity signBlockEntity, SignBlockEntityRenderState signBlockEntityRenderState, float f, Vec3d vec3d, ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlayCommand, CallbackInfo ci) {
        SignBlockEntityAccessor accessor = (SignBlockEntityAccessor)signBlockEntity;
        accessor.signedPaintings$reloadIfNeeded();

        SignBlockEntityRenderStateAccessor state = (SignBlockEntityRenderStateAccessor)signBlockEntityRenderState;
        state.signedPaintings$setFrontInfo(accessor.signedPaintings$getFrontPaintingInfo());
        state.signedPaintings$setBackInfo(accessor.signedPaintings$getBackPaintingInfo());
    }
}
