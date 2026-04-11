package com.nettakrim.signed_paintings.mixin;

import com.nettakrim.signed_paintings.access.SignBlockEntityRenderStateAccessor;
import com.nettakrim.signed_paintings.rendering.PaintingInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.access.SignBlockEntityAccessor;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignRenderer.class)
public abstract class SignBlockEntityRendererMixin implements BlockEntityRenderer<SignBlockEntity, SignRenderState> {
    @Inject(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/blockentity/AbstractSignRenderer;submitSign(Lcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/level/block/state/properties/WoodType;Lnet/minecraft/client/model/Model$Simple;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V"
            ),
            method = "submitSignWithText(Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/SignBlock;Lnet/minecraft/world/level/block/state/properties/WoodType;Lnet/minecraft/client/model/Model$Simple;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
            cancellable = true
    )
    private void onRender(SignRenderState renderState, PoseStack matrices, BlockState blockState, SignBlock block, WoodType woodType, Model.Simple model, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, SubmitNodeCollector queue, CallbackInfo ci) {
        if (renderPaintings(renderState, matrices, block, queue)) {
            matrices.popPose();
            ci.cancel();
        }
    }

    @Unique
    private boolean renderPaintings(SignRenderState renderState, PoseStack matrices, SignBlock block, SubmitNodeCollector queue) {
        if (!SignedPaintingsClient.renderSigns) return false;

        boolean success = false;
        SignBlockEntityRenderStateAccessor accessor = (SignBlockEntityRenderStateAccessor)renderState;
        success |= renderPaintingInfo(accessor.signedPaintings$getFrontInfo(), queue, matrices, renderState, renderState.frontText);
        success |= renderPaintingInfo(accessor.signedPaintings$getBackInfo(), queue, matrices, renderState, renderState.backText);
        return success;
    }

    @Unique
    private boolean renderPaintingInfo(PaintingInfo info, SubmitNodeCollector queue, PoseStack matrices, SignRenderState state, SignText text) {
        if (info != null && info.isReady()) {
            SignedPaintingsClient.paintingRenderer.renderOrQueuePainting(matrices, queue, info, text != null && text.hasGlowingText() ? -1 : state.lightCoords);
            return true;
        }
        return false;
    }

    public boolean isInRenderDistance(SignBlockEntity blockEntity, Vec3 pos) {
        return (hasPainting((SignBlockEntityAccessor)blockEntity) && SignedPaintingsClient.reduceCulling) || BlockEntityRenderer.super.shouldRender(blockEntity, pos);
    }

    @Unique
    private boolean hasPainting(SignBlockEntityAccessor accessor) {
        if (!SignedPaintingsClient.renderSigns) return false;
        PaintingInfo paintingInfo = accessor.signedPaintings$getFrontPaintingInfo();
        if (paintingInfo != null && paintingInfo.isReady()) return true;
        paintingInfo = accessor.signedPaintings$getBackPaintingInfo();
        return paintingInfo != null && paintingInfo.isReady();
    }

    @Inject(at = @At("TAIL"), method = "extractRenderState(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V")
    private void updateRenderState(SignBlockEntity signBlockEntity, SignRenderState signBlockEntityRenderState, float f, Vec3 vec3d, ModelFeatureRenderer.CrumblingOverlay crumblingOverlayCommand, CallbackInfo ci) {
        SignBlockEntityAccessor accessor = (SignBlockEntityAccessor)signBlockEntity;
        accessor.signedPaintings$reloadIfNeeded();

        SignBlockEntityRenderStateAccessor state = (SignBlockEntityRenderStateAccessor)signBlockEntityRenderState;
        state.signedPaintings$setFrontInfo(accessor.signedPaintings$getFrontPaintingInfo());
        state.signedPaintings$setBackInfo(accessor.signedPaintings$getBackPaintingInfo());
    }
}
