package com.nettakrim.signed_paintings.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.rendering.OverlayInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShieldSpecialRenderer.class)
public class ShieldModelRendererMixin {
    @Inject(at = @At("TAIL"), method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V")
    private void onShieldRender(DataComponentMap components, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor, CallbackInfo ci) {
        if (!SignedPaintingsClient.renderShields) return;

        Component name = components.get(DataComponents.CUSTOM_NAME);
        if (name == null) return;

        OverlayInfo info = SignedPaintingsClient.imageManager.getOverlayInfo(name.getString());

        if (info.isReady()) {
            SignedPaintingsClient.paintingRenderer.renderItemOverlay(poseStack, submitNodeCollector, info, lightCoords);
        }
    }
}
