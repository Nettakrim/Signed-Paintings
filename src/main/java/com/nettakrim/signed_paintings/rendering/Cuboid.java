package com.nettakrim.signed_paintings.rendering;

import org.joml.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.Math;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

public class Cuboid {
    private final Vector3fc size;
    private final Vector3fc offset;

    private Cuboid(float xSize, float ySize, float zSize, float xOffset, float yOffset, float zOffset) {
        this.size = new Vector3f(xSize, ySize, zSize);
        this.offset = new Vector3f(xOffset, yOffset, zOffset);
    }

    public static Cuboid CreateWallCuboid(float xSize, Centering.Type xCentering, float ySize, Centering.Type yCentering, float zSize) {
        return new Cuboid(xSize, ySize, zSize, Centering.getOffset(xSize, xCentering), Centering.getOffset(ySize, yCentering), -0.0025f + (zSize/2));
    }

    public static Cuboid CreateFlushCuboid(float xSize, Centering.Type xCentering, float ySize, Centering.Type yCentering, float zSize) {
        return new Cuboid(xSize, ySize, zSize, Centering.getOffset(xSize, xCentering), Centering.getOffset(ySize, yCentering), 1.0f - (zSize/2));
    }

    public static Cuboid CreateCentralCuboid(float xSize, Centering.Type xCentering, float ySize, Centering.Type yCentering, float zSize) {
        return new Cuboid(xSize, ySize, zSize, Centering.getOffset(xSize, xCentering), Centering.getOffset(ySize, yCentering), (zSize/2) - 0.0025f + 0.5f);
    }

    public static Cuboid CreateOverlayCuboid(float aspectRatio) {
        float width = 5/6f;
        float height = 5/3f;
        if (aspectRatio > 0.5f) {
            height /= aspectRatio*2;
        } else {
            width *= aspectRatio*2;
        }
        return new Cuboid(width, height, 1/8f, 0, -5/6f, 0);
    }

    public void renderFace(PoseStack.Pose matrix, VertexConsumer vertexConsumer, Vector3f face, boolean split, float minU, float maxU, float minV, float maxV, int light) {
        AxisAngle4f rotation;

        if (face.y == 0) {
            float angle = 0;
            if (face.z == 0) {
                angle = face.x < 0 ? Mth.HALF_PI : -Mth.HALF_PI;
            } else {
                if (face.z < 0) {
                    angle = Mth.PI;
                }
            }
            rotation = new AxisAngle4f(angle, 0, 1, 0);
        } else {
            float angle = face.y < 0 ? Mth.HALF_PI : -Mth.HALF_PI;
            rotation = new AxisAngle4f(angle, 1, 0, 0);
        }

        renderFaceRotated(matrix, vertexConsumer, rotation, split, minU, maxU, minV, maxV, light);
    }

    private Vector3f adjustVertex(Vector3f v, AxisAngle4f rotation) {
        Vector3f vertex = rotation.transform(v);
        vertex.mul(size);
        vertex.add(offset);
        return vertex;
    }

    private void renderFaceRotated(PoseStack.Pose matrix, VertexConsumer vertexConsumer, AxisAngle4f rotation, boolean split, float minU, float maxU, float minV, float maxV, int light) {
        Vector3f normal;
        if (light == -1) {
            light = 15728640;
            normal = new Vector3f(0, 1, 0);
        } else {
            normal = rotation.transform(new Vector3f(0, 0, 1));
        }

        if (!split) {
            renderQuad(matrix, vertexConsumer, -0.5f, 0.5f, -0.5f, 0.5f, rotation, minU, maxU, minV, maxV, normal, light);
            return;
        }

        Vector3f relevantSize = new Vector3f(size);
        rotation.transform(relevantSize);
        relevantSize.absolute();

        for (float minX = 0; minX < relevantSize.x; minX++) {
            for (float minY = 0; minY < relevantSize.y; minY++) {
                float maxX = Math.min(minX+1, relevantSize.x);
                float maxY = Math.min(minY+1, relevantSize.y);

                float scaledMinX = (minX/relevantSize.x)-0.5f;
                float scaledMaxX = (maxX/relevantSize.x)-0.5f;
                float scaledMinY = (minY/relevantSize.y)-0.5f;
                float scaledMaxY = (maxY/relevantSize.y)-0.5f;

                float newMaxU = minU+((maxU-minU)*(maxX-minX));
                float newMinV = maxV-((maxV-minV)*(maxY-minY));

                renderQuad(matrix, vertexConsumer, scaledMinX, scaledMaxX, scaledMinY, scaledMaxY, rotation, minU, newMaxU, newMinV, maxV, normal, light);
            }
        }
    }

    private void renderQuad(PoseStack.Pose matrix, VertexConsumer vertexConsumer, float minX, float maxX, float minY, float maxY, AxisAngle4f rotation, float minU, float maxU, float minV, float maxV, Vector3f normal, int light) {
        vertexFromVector(matrix, vertexConsumer, adjustVertex(new Vector3f(minX, minY, 0.5f), rotation), minU, maxV, normal, light);
        vertexFromVector(matrix, vertexConsumer, adjustVertex(new Vector3f(maxX, minY, 0.5f), rotation), maxU, maxV, normal, light);
        vertexFromVector(matrix, vertexConsumer, adjustVertex(new Vector3f(maxX, maxY, 0.5f), rotation), maxU, minV, normal, light);
        vertexFromVector(matrix, vertexConsumer, adjustVertex(new Vector3f(minX, maxY, 0.5f), rotation), minU, minV, normal, light);
    }

    private void vertexFromVector(PoseStack.Pose matrix, VertexConsumer vertexConsumer, Vector3f vertexPos, float u, float v, Vector3f normal, int light) {
        this.vertex(matrix, vertexConsumer, vertexPos.x, vertexPos.y, vertexPos.z, u, v, normal.x, normal.y, normal.z, light);
    }

    private void vertex(PoseStack.Pose matrix, VertexConsumer vertexConsumer, float x, float y, float z, float u, float v, float normalX, float normalY, float normalZ, int light) {
        vertexConsumer.addVertex(matrix.pose(), x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix, normalX, normalY, normalZ);
    }
}
