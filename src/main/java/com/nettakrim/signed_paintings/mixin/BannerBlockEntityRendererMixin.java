package com.nettakrim.signed_paintings.mixin;

import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.access.OverlayInfoAccessor;
import com.nettakrim.signed_paintings.rendering.OverlayInfo;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BannerBlockEntityRenderer.class)
public class BannerBlockEntityRendererMixin {
    @Final
    @Shadow
    private ModelPart banner;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V", ordinal = 1), method = "render(Lnet/minecraft/block/entity/BannerBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V")
    private void onRender(BannerBlockEntity bannerBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j, CallbackInfo ci) {
        OverlayInfoAccessor accessor = (OverlayInfoAccessor)bannerBlockEntity;
        accessor.signedPaintings$reloadIfNeeded();
        OverlayInfo overlayInfo = accessor.signedPaintings$getOverlayInfo();
        if (overlayInfo.isReady()) {
            SignedPaintingsClient.paintingRenderer.renderImageOverlay(matrixStack, vertexConsumerProvider, overlayInfo, banner, i);
        }
    }
}
