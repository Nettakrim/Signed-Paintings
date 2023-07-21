package com.nettakrim;

import net.minecraft.client.model.Model;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;

public class PaintingRenderer {
    public PaintingRenderer() {

    }

    public void renderPainting(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Model model, PaintingInfo info, int light, float rotationDegrees) {
        matrices.push();
        matrices.translate(0.5F, 0.5F, 0.5F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationDegrees));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(info.rotation));
        info.cuboid.setupRendering(matrices);

        VertexConsumer imageVertexConsumer = vertexConsumers.getBuffer(model.getLayer(info.getImageIdentifier()));
        renderImage(imageVertexConsumer, info, light);

        SpriteIdentifier backSprite = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, info.getBackIdentifier());
        VertexConsumer backVertexConsumer = backSprite.getVertexConsumer(vertexConsumers, model::getLayer);
        renderBack(backVertexConsumer, backSprite.getSprite(), info, light);

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
