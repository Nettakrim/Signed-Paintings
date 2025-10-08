package com.nettakrim.signed_paintings.mixin;

import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.access.BannerBlockEntityRenderStateAccessor;
import com.nettakrim.signed_paintings.access.OverlayInfoAccessor;
import com.nettakrim.signed_paintings.rendering.OverlayInfo;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.block.entity.model.BannerBlockModel;
import net.minecraft.client.render.block.entity.model.BannerFlagBlockModel;
import net.minecraft.client.render.block.entity.state.BannerBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BannerBlockEntityRenderer.class)
public class BannerBlockEntityRendererMixin {
    @Unique
    private static OverlayInfo currentOverlayInfo;

    @Inject(at = @At("HEAD"), method = "updateRenderState(Lnet/minecraft/block/entity/BannerBlockEntity;Lnet/minecraft/client/render/block/entity/state/BannerBlockEntityRenderState;FLnet/minecraft/util/math/Vec3d;Lnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V")
    private void updateRenderState(BannerBlockEntity bannerBlockEntity, BannerBlockEntityRenderState bannerBlockEntityRenderState, float f, Vec3d vec3d, ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlayCommand, CallbackInfo ci) {
        OverlayInfoAccessor accessor = (OverlayInfoAccessor)bannerBlockEntity;
        accessor.signedPaintings$reloadIfNeeded();
        ((BannerBlockEntityRenderStateAccessor)bannerBlockEntityRenderState).signedPaintings$setOverlayInfo(accessor.signedPaintings$getOverlayInfo());
    }

    @Inject(at = @At("HEAD"), method = "render(Lnet/minecraft/client/render/block/entity/state/BannerBlockEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V")
    private void prepareOverlay(BannerBlockEntityRenderState bannerBlockEntityRenderState, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState, CallbackInfo ci) {
        currentOverlayInfo = ((BannerBlockEntityRenderStateAccessor)bannerBlockEntityRenderState).signedPaintings$getOverlayInfo();
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/entity/BannerBlockEntityRenderer;renderCanvas(Lnet/minecraft/client/texture/SpriteHolder;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IILnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/SpriteIdentifier;ZLnet/minecraft/util/DyeColor;Lnet/minecraft/component/type/BannerPatternsComponent;ZLnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V"), method = "render(Lnet/minecraft/client/texture/SpriteHolder;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IIFLnet/minecraft/client/render/block/entity/model/BannerBlockModel;Lnet/minecraft/client/render/block/entity/model/BannerFlagBlockModel;FLnet/minecraft/util/DyeColor;Lnet/minecraft/component/type/BannerPatternsComponent;Lnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V")
    private static void onRender(SpriteHolder materials, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay, float yaw, BannerBlockModel model, BannerFlagBlockModel flagModel, float pitch, DyeColor dyeColor, BannerPatternsComponent bannerPatterns, ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay, int i, CallbackInfo ci) {
        if (!SignedPaintingsClient.renderBanners || currentOverlayInfo == null) return;

        if (currentOverlayInfo.isReady()) {
            SignedPaintingsClient.paintingRenderer.renderImageOverlay(matrices, queue, currentOverlayInfo, light, flagModel, pitch);
        }
        currentOverlayInfo = null;
    }
}
