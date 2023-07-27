package com.nettakrim.signed_paintings.mixin;

import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.rendering.OverlayInfo;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

@Mixin(BuiltinModelItemRenderer.class)
public class BuiltinModelItemRendererMixin {
    @Unique
    private final HashMap<String, OverlayInfo> itemNameToOverlay = new HashMap<>();

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V", ordinal = 0), method = "render")
    private void onShieldRender(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, CallbackInfo ci) {
        if (!stack.hasCustomName()) return;
        String name = stack.getName().getString();
        OverlayInfo info = itemNameToOverlay.get(name);
        if (info == null) {
            info = new OverlayInfo();
            info.loadOverlay(name);
            itemNameToOverlay.put(name, info);
        }
        if (info.isReady()) {
            SignedPaintingsClient.paintingRenderer.renderItemOverlay(matrices, vertexConsumers, info, light);
        }
    }
}
