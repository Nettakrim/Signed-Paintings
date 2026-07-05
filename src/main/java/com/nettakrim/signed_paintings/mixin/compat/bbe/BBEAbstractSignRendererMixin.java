package com.nettakrim.signed_paintings.mixin.compat.bbe;

import betterblockentities.client.render.immediate.blockentity.renderers.BBEAbstractSignRenderer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.access.SignBlockEntityAccessor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BBEAbstractSignRenderer.class)
public abstract class BBEAbstractSignRendererMixin implements BlockEntityRenderer<SignBlockEntity, SignRenderState> {
    @WrapMethod(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V")
    private void onRender(SignRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, Operation<Void> original) {
        if (SignedPaintingsClient.paintingRenderer.renderSignPaintings(state, poseStack, submitNodeCollector)) {
            return;
        }

        original.call(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    public boolean shouldRender(@NonNull SignBlockEntity blockEntity, @NonNull Vec3 cameraPosition) {
        return SignedPaintingsClient.paintingRenderer.renderWithReducedCulling((SignBlockEntityAccessor)blockEntity) || BlockEntityRenderer.super.shouldRender(blockEntity, cameraPosition);
    }

    @Inject(at = @At("TAIL"), method = "extractRenderState(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V")
    private void updateRenderState(SignBlockEntity signBlockEntity, SignRenderState signBlockEntityRenderState, float f, Vec3 vec3d, ModelFeatureRenderer.CrumblingOverlay crumblingOverlayCommand, CallbackInfo ci) {
        SignedPaintingsClient.paintingRenderer.modifySignRenderState(signBlockEntity, signBlockEntityRenderState);
    }
}
