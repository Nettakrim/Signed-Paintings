package com.nettakrim.signed_paintings.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.rendering.OverlayInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShieldSpecialRenderer.class)
public class ShieldModelRendererMixin {
    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", ordinal = 0), method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V")
    private void onShieldRender(DataComponentMap componentMap, ItemDisplayContext itemDisplayContext, PoseStack matrixStack, SubmitNodeCollector orderedRenderCommandQueue, int i, int j, boolean bl, int k, CallbackInfo ci) {
        if (!SignedPaintingsClient.renderShields) return;

        Component name = componentMap.getOrDefault(DataComponents.CUSTOM_NAME, null);
        if (name == null) return;

        OverlayInfo info = SignedPaintingsClient.imageManager.getOverlayInfo(name.getString());

        if (info.isReady()) {
            SignedPaintingsClient.paintingRenderer.renderItemOverlay(matrixStack, orderedRenderCommandQueue, info, i);
        }
    }
}
