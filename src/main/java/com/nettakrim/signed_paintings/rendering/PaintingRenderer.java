package com.nettakrim.signed_paintings.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nettakrim.signed_paintings.util.ImageManager;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

public class PaintingRenderer {
    public PaintingRenderer() {}

    /*
    private record TranslucentRenderData(MatrixStack.Entry matrixEntry, PaintingInfo info, int light) {}

    private static final List<TranslucentRenderData> translucentQueue = new ArrayList<>();

    public void renderTranslucentQueue(VertexConsumerProvider vertexConsumers) {
        for (TranslucentRenderData data : translucentQueue) {
            renderTranslucentPaintingImmediately(vertexConsumers, data);
        }
        translucentQueue.clear();
    }

    private static void queueTranslucentRender(MatrixStack.Entry capturedEntry, PaintingInfo info, int light) {
        translucentQueue.add(new TranslucentRenderData(capturedEntry, info, light));
    }

    private void renderTranslucentPaintingImmediately(VertexConsumerProvider consumers, TranslucentRenderData data) {
        Identifier image = data.info.getImageIdentifier();
        if (!ImageManager.hasImage(image)) return;

        renderPaintingImmediately(data.matrixEntry, consumers, data.info, data.light, RenderLayer.getEntityTranslucent(image));
    }

    private void renderPaintingImmediately(MatrixStack.Entry matrix, VertexConsumerProvider consumers, PaintingInfo info, int light, RenderLayer renderLayer) {
        renderImage(matrix, consumers.getBuffer(renderLayer), info, light);

        if (info.getBackType() != BackType.Type.NONE) {
            Sprite sprite = info.getBackSprite();
            renderBack(matrix, sprite.getTextureSpecificVertexConsumer(consumers.getBuffer(RenderLayer.getEntityCutout(sprite.getAtlasId()))), sprite, info, light);
        }
    }
    */

    public void renderOrQueuePainting(PoseStack matrices, float rotation, SubmitNodeCollector queue, PaintingInfo info, int light) {
        Identifier image = info.getImageIdentifier();
        if (!ImageManager.hasImage(image)) return;

        matrices.pushPose();
        matrices.translate(info.offsetVec.x + 0.5f, info.offsetVec.y + 0.5f, info.offsetVec.z);
        matrices.mulPose(Axis.YP.rotationDegrees(info.rotationVec.y + (info.isFront ? 0 : 180) - rotation));
        matrices.mulPose(Axis.ZP.rotationDegrees(info.rotationVec.z));
        matrices.mulPose(Axis.XP.rotationDegrees(info.rotationVec.x));

        if (info.hasTranslucency()) {
            // TODO: this
            // queueing seems to cause more problems than it solves (i think its currently not correctly happening after everything)
            //queueTranslucentRender(matrices.peek().copy(), info, light);
            renderPainting(matrices, queue, info, light, RenderTypes.entityTranslucent(info.getImageIdentifier()));
        } else {
            renderPainting(matrices, queue, info, light, RenderTypes.entityCutout(info.getImageIdentifier()));
        }
        matrices.popPose();
    }

    private void renderPainting(PoseStack matrices, SubmitNodeCollector queue, PaintingInfo info, int light, RenderType renderLayer) {
        queue.submitCustomGeometry(matrices, renderLayer, (matrix, vertexConsumer) -> renderImage(matrix, vertexConsumer, info, light));

        if (info.getBackType() != BackType.Type.NONE) {
            TextureAtlasSprite sprite = info.getBackSprite();
            queue.submitCustomGeometry(matrices, RenderTypes.entityCutout(sprite.atlasLocation()), (matrix, vertexConsumer) -> renderBack(matrix, sprite.wrap(vertexConsumer), sprite, info, light));
        }
    }

    private void renderImage(PoseStack.Pose matrix, VertexConsumer vertexConsumer, PaintingInfo info, int light) {
        info.cuboid.renderFace(matrix, vertexConsumer, new Vector3f(0, 0, 1), false, 0, 1, 0, 1, light);
    }

    private void renderBack(PoseStack.Pose matrix, VertexConsumer vertexConsumer, TextureAtlasSprite backSprite, PaintingInfo info, int light) {
        info.cuboid.renderFace(matrix, vertexConsumer, new Vector3f(0,  0,  -1), true, backSprite.getU0(), backSprite.getU1(), backSprite.getV0(), backSprite.getV1(), light);

        info.cuboid.renderFace(matrix, vertexConsumer, new Vector3f(1,  0,  0),  true, backSprite.getU0(), backSprite.getU1(), backSprite.getV0(), backSprite.getV1(), light);
        info.cuboid.renderFace(matrix, vertexConsumer, new Vector3f(-1, 0,  0),  true, backSprite.getU0(), backSprite.getU1(), backSprite.getV0(), backSprite.getV1(), light);

        info.cuboid.renderFace(matrix, vertexConsumer, new Vector3f(0,  1,  0),  true, backSprite.getU0(), backSprite.getU1(), backSprite.getV0(), backSprite.getV1(), light);
        info.cuboid.renderFace(matrix, vertexConsumer, new Vector3f(0,  -1, 0),  true, backSprite.getU0(), backSprite.getU1(), backSprite.getV0(), backSprite.getV1(), light);
    }

    public void renderImageOverlay(PoseStack matrices, SubmitNodeCollector queue, OverlayInfo info, int light, BannerFlagModel flagBlockModel, float pitch) {
        matrices.pushPose();
        flagBlockModel.setupAnim(pitch);
        flagBlockModel.root().getChild("flag").translateAndRotate(matrices);
        //these numbers are entirely trial and error, I have no idea how to derive them
        matrices.scale(1.5f, -1.5f, 1f);
        matrices.translate(0, 0, -0.2f);
        renderOverlay(matrices, queue, info, light);
        matrices.popPose();
    }

    public void renderItemOverlay(PoseStack matrices, SubmitNodeCollector queue, OverlayInfo info, int light) {
        matrices.pushPose();
        //these are also trial and error
        matrices.scale(0.75f, -0.75f, -1f);
        matrices.translate(0F, 0.833f, 0.065f);
        renderOverlay(matrices, queue, info, light);
        matrices.popPose();
    }

    private void renderOverlay(PoseStack matrices, SubmitNodeCollector queue, OverlayInfo info, int light) {
        Identifier image = info.getImageIdentifier();
        if (!ImageManager.hasImage(image)) return;

        RenderType layer = info.hasTranslucency() ? RenderTypes.entityTranslucent(image) : RenderTypes.entityCutout(image);
        queue.submitCustomGeometry(matrices, layer, (matrix, vertexConsumer) -> info.cuboid.renderFace(matrix, vertexConsumer, new Vector3f(0, 0, 1), false, 0, 1, 0, 1, light));
    }
}