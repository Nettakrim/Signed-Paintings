package com.nettakrim.signed_paintings.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.access.BannerBlockEntityRenderStateAccessor;
import com.nettakrim.signed_paintings.access.OverlayInfoAccessor;
import com.nettakrim.signed_paintings.rendering.OverlayInfo;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.model.object.banner.BannerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BannerRenderer.class)
public class BannerBlockEntityRendererMixin {
    @Unique
    private static OverlayInfo currentOverlayInfo;

    @Inject(at = @At("HEAD"), method = "extractRenderState(Lnet/minecraft/world/level/block/entity/BannerBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/BannerRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V")
    private void updateRenderState(BannerBlockEntity blockEntity, BannerRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress, CallbackInfo ci) {
        OverlayInfoAccessor accessor = (OverlayInfoAccessor) blockEntity;
        accessor.signedPaintings$reloadIfNeeded();
        ((BannerBlockEntityRenderStateAccessor) state).signedPaintings$setOverlayInfo(accessor.signedPaintings$getOverlayInfo());
    }

    @Inject(at = @At("HEAD"), method = "submit(Lnet/minecraft/client/renderer/blockentity/state/BannerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V")
    private void prepareOverlay(BannerRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, net.minecraft.client.renderer.state.level.CameraRenderState camera, CallbackInfo ci) {
        currentOverlayInfo = ((BannerBlockEntityRenderStateAccessor)state).signedPaintings$getOverlayInfo();
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BannerRenderer;submitPatterns(Lnet/minecraft/client/resources/model/sprite/SpriteGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IILnet/minecraft/client/model/Model;Ljava/lang/Object;ZLnet/minecraft/world/item/DyeColor;Lnet/minecraft/world/level/block/entity/BannerPatternLayers;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"), method = "submitBanner")
    private static void onRender(SpriteGetter sprites, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, BannerModel model, BannerFlagModel flagModel, float phase, DyeColor baseColor, BannerPatternLayers patterns, ModelFeatureRenderer.CrumblingOverlay breakProgress, int outlineColor, CallbackInfo ci) {
        if (!SignedPaintingsClient.renderBanners || currentOverlayInfo == null) return;

        if (currentOverlayInfo.isReady()) {
            SignedPaintingsClient.paintingRenderer.renderImageOverlay(poseStack, submitNodeCollector, currentOverlayInfo, lightCoords, flagModel, phase);
        }
        currentOverlayInfo = null;
    }
}
