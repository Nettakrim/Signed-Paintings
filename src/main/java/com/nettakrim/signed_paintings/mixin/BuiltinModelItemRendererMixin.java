package com.nettakrim.signed_paintings.mixin;

import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.rendering.OverlayInfo;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.model.special.ShieldModelRenderer;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShieldModelRenderer.class)
public class BuiltinModelItemRendererMixin {
    // TODO: update shield rendering

    //@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V", ordinal = 0), method = "render(Lnet/minecraft/component/ComponentMap;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IIZ)V")
    //private void onShieldRender(ComponentMap componentMap, ItemDisplayContext itemDisplayContext, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, int j, boolean bl, CallbackInfo ci) {
    //    if (!SignedPaintingsClient.renderShields) return;
    //
    //    Text name = componentMap.getOrDefault(DataComponentTypes.CUSTOM_NAME, null);
    //    if (name == null) return;
    //
    //    OverlayInfo info = SignedPaintingsClient.imageManager.getOverlayInfo(name.getString());
    //
    //    if (info.isReady()) {
    //        SignedPaintingsClient.paintingRenderer.renderItemOverlay(matrixStack, vertexConsumerProvider, info, light);
    //    }
    //}
}
