package com.nettakrim.mixin;

import com.nettakrim.PaintingInfo;
import com.nettakrim.access.AbstractSignBlockAccessor;
import net.minecraft.block.*;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignBlockEntityRenderer.class)
public abstract class SignBlockEntityRendererMixin {
    @Inject(
            at = @At(
                    value = "HEAD",
                    target = "Lnet/minecraft/client/render/block/entity/SignBlockEntityRenderer;renderSign(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/block/WoodType;Lnet/minecraft/client/model/Model;)V"
            ),
            method = "render(Lnet/minecraft/block/entity/SignBlockEntity;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/block/BlockState;Lnet/minecraft/block/AbstractSignBlock;Lnet/minecraft/block/WoodType;Lnet/minecraft/client/model/Model;)V",
            cancellable = false
    )
    private void onRender(SignBlockEntity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, BlockState state, AbstractSignBlock block, WoodType woodType, Model model, CallbackInfo ci) {
        PaintingInfo info = ((AbstractSignBlockAccessor)block).getPaintingInfo();

        if (info != null && info.isReady()) {
            if (!(state.getBlock() instanceof SignBlock)) {
                //wall sign
            }

            renderPainting(matrices, vertexConsumers, model, info, light, -block.getRotationDegrees(state));
            //if (--is image--) {
            //    ci.cancel();
            //}
        }
    }

    private void renderPainting(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Model model, PaintingInfo info, int light, float rotationDegrees) {
        matrices.push();
        matrices.translate(0.5F, 0.5F, 0.5F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationDegrees));
        info.cuboid.setupRendering(matrices);

        VertexConsumer imageVertexConsumer = vertexConsumers.getBuffer(model.getLayer(info.image.identifier));
        renderImage(imageVertexConsumer, info, light);

        SpriteIdentifier woodSprite = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, info.back);
        VertexConsumer backVertexConsumer = woodSprite.getVertexConsumer(vertexConsumers, model::getLayer);
        renderBack(backVertexConsumer, woodSprite.getSprite(), info, light);

        matrices.pop();
    }

    private void renderImage(VertexConsumer vertexConsumer, PaintingInfo info, int light) {
        info.cuboid.renderFace(vertexConsumer, new Vector3f(0, 0, 1), false, 0, 1, 0, 1, light);
    }

    private void renderBack(VertexConsumer vertexConsumer, Sprite backSprite, PaintingInfo info, int light) {
        info.cuboid.renderFace(vertexConsumer, new Vector3f(0,  0,  -1), true, backSprite.getMinU(), backSprite.getMaxU(), backSprite.getMinV(), backSprite.getMaxV(), light);

        info.cuboid.renderFace(vertexConsumer, new Vector3f(1,  0,  0),  true, backSprite.getMinU(), backSprite.getMaxU(), backSprite.getMinV(), backSprite.getMaxV(), light);
        info.cuboid.renderFace(vertexConsumer, new Vector3f(-1, 0,  0),  true, backSprite.getMinU(), backSprite.getMaxU(), backSprite.getMinV(), backSprite.getMaxV(), light);

        info.cuboid.renderFace(vertexConsumer, new Vector3f(0,  1,  0),  true, backSprite.getMinU(), backSprite.getMaxU(), backSprite.getMinV(), backSprite.getMaxV(), light);
        info.cuboid.renderFace(vertexConsumer, new Vector3f(0,  -1, 0),  true, backSprite.getMinU(), backSprite.getMaxU(), backSprite.getMinV(), backSprite.getMaxV(), light);
    }
}
