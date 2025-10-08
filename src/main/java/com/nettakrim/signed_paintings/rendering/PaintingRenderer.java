package com.nettakrim.signed_paintings.rendering;

import com.nettakrim.signed_paintings.util.ImageManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.model.BannerFlagBlockModel;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;
import java.util.List;
import java.util.ArrayList;

public class PaintingRenderer {
    public PaintingRenderer() {

    }
    private record TranslucentRenderData(MatrixStack.Entry matrixEntry, PaintingInfo info, int light) {}


    private static final List<TranslucentRenderData> translucentQueue = new ArrayList<>();

    private static void queueTranslucentRender(MatrixStack.Entry capturedEntry, PaintingInfo info, int light) {
        translucentQueue.add(new TranslucentRenderData(capturedEntry, info, light));
    }

    private void renderTranslucentPaintingImmediately(MatrixStack matrices, VertexConsumerProvider consumers, TranslucentRenderData data) {
        Identifier image = data.info.getImageIdentifier();
        if (!ImageManager.hasImage(image)) return;

        matrices.push();
        matrices.multiplyPositionMatrix(data.matrixEntry.getPositionMatrix());
        //renderPainting(consumers, data.info, data.light, RenderLayer.getEntityTranslucent(image));
        matrices.pop(); 
    }


    public void renderTranslucentQueue(MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        for (TranslucentRenderData data : translucentQueue) {
            renderTranslucentPaintingImmediately(matrices, vertexConsumers, data);
        }
        translucentQueue.clear();
    }

    public void renderOrQueuePainting(MatrixStack matrices, OrderedRenderCommandQueue queue, PaintingInfo info, int light) {
        Identifier image = info.getImageIdentifier();
        if (!ImageManager.hasImage(image)) return;

        matrices.push();
        matrices.translate(info.offsetVec.x, info.offsetVec.y, info.offsetVec.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(info.rotationVec.y + (info.isFront ? 0 : 180)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(info.rotationVec.z));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(info.rotationVec.x));

        if (info.hasPartialTransparency()) {
            queueTranslucentRender(matrices.peek().copy(), info, light);
        } else {
            renderPainting(matrices, queue, info, light, RenderLayer.getEntityCutout(info.getImageIdentifier()));
        }
        matrices.pop();
    }

    private void renderPainting(MatrixStack matrices, OrderedRenderCommandQueue queue, PaintingInfo info, int light, RenderLayer renderLayer) {
        queue.submitCustom(matrices, renderLayer, (matrix, vertexConsumer) -> renderImage(matrix, vertexConsumer, info, light));

        if (info.getBackType() != BackType.Type.NONE) {
            Sprite sprite = info.getBackSprite();
            queue.submitCustom(matrices, RenderLayer.getEntityCutout(sprite.getAtlasId()), (matrix, vertexConsumer) -> renderBack(matrix, sprite.getTextureSpecificVertexConsumer(vertexConsumer), sprite, info, light));
        }
    }

    private void renderImage(MatrixStack.Entry matrix, VertexConsumer vertexConsumer, PaintingInfo info, int light) {
        info.cuboid.renderFace(matrix, vertexConsumer, new Vector3f(0, 0, 1), false, 0, 1, 0, 1, light);
    }

    private void renderBack(MatrixStack.Entry matrix, VertexConsumer vertexConsumer, Sprite backSprite, PaintingInfo info, int light) {
        info.cuboid.renderFace(matrix, vertexConsumer, new Vector3f(0,  0,  -1), true, backSprite.getMinU(), backSprite.getMaxU(), backSprite.getMinV(), backSprite.getMaxV(), light);

        info.cuboid.renderFace(matrix, vertexConsumer, new Vector3f(1,  0,  0),  true, backSprite.getMinU(), backSprite.getMaxU(), backSprite.getMinV(), backSprite.getMaxV(), light);
        info.cuboid.renderFace(matrix, vertexConsumer, new Vector3f(-1, 0,  0),  true, backSprite.getMinU(), backSprite.getMaxU(), backSprite.getMinV(), backSprite.getMaxV(), light);

        info.cuboid.renderFace(matrix, vertexConsumer, new Vector3f(0,  1,  0),  true, backSprite.getMinU(), backSprite.getMaxU(), backSprite.getMinV(), backSprite.getMaxV(), light);
        info.cuboid.renderFace(matrix, vertexConsumer, new Vector3f(0,  -1, 0),  true, backSprite.getMinU(), backSprite.getMaxU(), backSprite.getMinV(), backSprite.getMaxV(), light);
    }

    public void renderImageOverlay(MatrixStack matrices, OrderedRenderCommandQueue queue, OverlayInfo info, int light, BannerFlagBlockModel flagBlockModel, float pitch) {
        Identifier image = info.getImageIdentifier();
        if (!ImageManager.hasImage(image)) return;

        matrices.push();
        flagBlockModel.setAngles(pitch);
        flagBlockModel.getRootPart().getChild("flag").applyTransform(matrices);
        //these numbers are entirely trial and error, I have no idea how to derive them
        matrices.scale(1.5f, -1.5f, 1f);
        matrices.translate(0, 0, -0.2f);

        RenderLayer layer = info.hasPartialTransparency() ? RenderLayer.getEntityTranslucent(image) : RenderLayer.getEntityCutout(image);
        queue.submitCustom(matrices, layer, (matrix, vertexConsumer) -> info.cuboid.renderFace(matrix, vertexConsumer, new Vector3f(0, 0, 1), false, 0, 1, 0, 1, light));

        matrices.pop();
    }


    public void renderItemOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, OverlayInfo info, int light) {
        Identifier image = info.getImageIdentifier();
        if (!ImageManager.hasImage(image)) return;

        RenderLayer layer = info.hasPartialTransparency() ? RenderLayer.getEntityTranslucent(image) : RenderLayer.getEntityCutout(image);
        VertexConsumer imageVertexConsumer = vertexConsumers.getBuffer(layer);

        matrices.push();
        //these are also trial and error
        matrices.scale(0.75f, -0.75f, -1f);
        matrices.translate(0F, 0.833f, 0.065f);
        //info.cuboid.renderFace(imageVertexConsumer, new Vector3f(0, 0, 1), false, 0, 1, 0, 1, light);
        matrices.pop();
    }
}