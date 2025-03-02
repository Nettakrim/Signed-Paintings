package com.nettakrim.signed_paintings.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.access.OverlayInfoAccessor;
import com.nettakrim.signed_paintings.rendering.OverlayInfo;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.block.entity.model.BannerBlockModel;
import net.minecraft.client.render.block.entity.model.BannerFlagBlockModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.util.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BannerBlockEntityRenderer.class)
public class BannerBlockEntityRendererMixin {
    //TODO: FIX RENDERING

    //@Unique
    //private static OverlayInfo currentOverlayInfo;

    //@Inject(at = @At("HEAD"), method = "render(Lnet/minecraft/block/entity/BannerBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V")
    //private void prepareRender(BannerBlockEntity bannerBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j, CallbackInfo ci) {
    //    OverlayInfoAccessor accessor = (OverlayInfoAccessor)bannerBlockEntity;
    //    accessor.signedPaintings$reloadIfNeeded();
    //    currentOverlayInfo = accessor.signedPaintings$getOverlayInfo();
    //}

    //@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/entity/BannerBlockEntityRenderer;renderCanvas(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/SpriteIdentifier;ZLnet/minecraft/util/DyeColor;Lnet/minecraft/component/type/BannerPatternsComponent;)V"), method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IIFLnet/minecraft/client/render/block/entity/model/BannerBlockModel;Lnet/minecraft/client/render/block/entity/model/BannerFlagBlockModel;FLnet/minecraft/util/DyeColor;Lnet/minecraft/component/type/BannerPatternsComponent;)V")
    //private static void onRender(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, float rotation, BannerBlockModel model, BannerFlagBlockModel flagModel, float sway, DyeColor baseColor, BannerPatternsComponent patterns, CallbackInfo ci) {
    //    if (!SignedPaintingsClient.renderBanners || currentOverlayInfo == null) return;

    //    if (currentOverlayInfo.isReady()) {
    //        SignedPaintingsClient.paintingRenderer.renderImageOverlay(matrices, vertexConsumers, currentOverlayInfo, model.getRootPart(), light);
    //    }
    //    currentOverlayInfo = null;
    //}
}
