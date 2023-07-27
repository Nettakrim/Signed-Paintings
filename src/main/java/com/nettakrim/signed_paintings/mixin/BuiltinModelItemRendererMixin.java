package com.nettakrim.signed_paintings.mixin;

import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.rendering.OverlayInfo;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BuiltinModelItemRenderer.class)
public class BuiltinModelItemRendererMixin {
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V", ordinal = 0), method = "render")
    private void onShieldRender(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, CallbackInfo ci) {
        //dont do this every frame
        OverlayInfo temp = new OverlayInfo();
        temp.loadOverlay(stack.getName().getString());
        if (temp.isReady()) {
            matrices.scale(1.0F, -1.0F, -1.0F);
            SignedPaintingsClient.paintingRenderer.renderItemOverlay(matrices, vertexConsumers, temp, light);
        }
        //OverlayInfoAccessor accessor = (OverlayInfoAccessor)stack.getItem();
        //OverlayInfo overlayInfo = accessor.signedPaintings$getOverlayInfo();
        //if (overlayInfo.isReady()) {
        //    SignedPaintingsClient.paintingRenderer.renderItemOverlay(matrices, vertexConsumers, overlayInfo, light);
        //}
    }
}
