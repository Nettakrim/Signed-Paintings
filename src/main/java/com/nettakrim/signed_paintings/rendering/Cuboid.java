package com.nettakrim.signed_paintings.rendering;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.*;

import java.lang.Math;

public class Cuboid {
    private final Vector3fc size;
    private final Vector3fc offset;

    private Matrix4f positionCache;
    private Matrix3f normalCache;

    public Cuboid(float xSize, float ySize, float zSize, float xOffset, float yOffset, float zOffset) {
        this.size = new Vector3f(xSize, ySize, zSize);
        this.offset = new Vector3f(xOffset, yOffset, zOffset);
    }

    public static Cuboid CreateWallCuboid(float xSize, Centering.Type xCentering, float ySize, Centering.Type yCentering, float zSize, float xOffset, float yOffset, float zOffset) {
        return new Cuboid(xSize, ySize, zSize, Centering.getOffset(xSize, xCentering)+xOffset, Centering.getOffset(ySize, yCentering)+yOffset, -0.5f + (zSize/2) + zOffset);
    }

    public static Cuboid CreateFlushCuboid(float xSize, Centering.Type xCentering, float ySize, Centering.Type yCentering, float zSize, float xOffset, float yOffset, float zOffset) {
        return new Cuboid(xSize, ySize, zSize, Centering.getOffset(xSize, xCentering)+xOffset, Centering.getOffset(ySize, yCentering)+yOffset, 0.5f - (zSize/2) + zOffset);
    }

    public static Cuboid CreateCentralCuboid(float xSize, Centering.Type xCentering, float ySize, Centering.Type yCentering, float zSize, float xOffset, float yOffset, float zOffset) {
        return new Cuboid(xSize, ySize, zSize, Centering.getOffset(xSize, xCentering)+xOffset, Centering.getOffset(ySize, yCentering)+yOffset, (zSize/2) + zOffset);
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

    public void setupRendering(MatrixStack matrices) {
        MatrixStack.Entry entry = matrices.peek();
        this.positionCache = entry.getPositionMatrix();
        this.normalCache = entry.getNormalMatrix();
    }

    public void renderFace(VertexConsumer vertexConsumer, Vector3f face, boolean split, float minU, float maxU, float minV, float maxV, int light) {
        AxisAngle4f rotation;

        if (face.y == 0) {
            float angle = 0;
            if (face.z == 0) {
                angle = face.x < 0 ? MathHelper.HALF_PI : -MathHelper.HALF_PI;
            } else {
                if (face.z < 0) {
                    angle = MathHelper.PI;
                }
            }
            rotation = new AxisAngle4f(angle, 0, 1, 0);
        } else {
            float angle = face.y < 0 ? MathHelper.HALF_PI : -MathHelper.HALF_PI;
            rotation = new AxisAngle4f(angle, 1, 0, 0);
        }

        renderFaceRotated(vertexConsumer, rotation, split, minU, maxU, minV, maxV, light);
    }

    private Vector3f adjustVertex(Vector3f v, AxisAngle4f rotation) {
        Vector3f vertex = rotation.transform(v);
        vertex.mul(size);
        vertex.add(offset);
        return vertex;
    }

    private void renderFaceRotated(VertexConsumer vertexConsumer, AxisAngle4f rotation, boolean split, float minU, float maxU, float minV, float maxV, int light) {
        Vector3f normal = rotation.transform(new Vector3f(0, 0, 1));

        if (!split) {
            renderQuad(vertexConsumer, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, rotation, minU, maxU, minV, maxV, normal, light);
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
                float newMaxV = minV+((maxV-minV)*(maxY-minY));

                renderQuad(vertexConsumer, scaledMinX, scaledMaxX, scaledMinY, scaledMaxY, 0.5f, rotation, minU, newMaxU, minV, newMaxV, normal, light);
            }
        }
    }

    private void renderQuad(VertexConsumer vertexConsumer, float minX, float maxX, float minY, float maxY, float z, AxisAngle4f rotation, float minU, float maxU, float minV, float maxV, Vector3f normal, int light) {
        vertexFromVector(vertexConsumer, adjustVertex(new Vector3f(minX, minY, z), rotation), minU, maxV, normal, light);
        vertexFromVector(vertexConsumer, adjustVertex(new Vector3f(maxX, minY, z), rotation), maxU, maxV, normal, light);
        vertexFromVector(vertexConsumer, adjustVertex(new Vector3f(maxX, maxY, z), rotation), maxU, minV, normal, light);
        vertexFromVector(vertexConsumer, adjustVertex(new Vector3f(minX, maxY, z), rotation), minU, minV, normal, light);
    }

    private void vertexFromVector(VertexConsumer vertexConsumer, Vector3f vertexPos, float u, float v, Vector3f normal, int light) {
        this.vertex(vertexConsumer, vertexPos.x, vertexPos.y, vertexPos.z, u, v, normal.x, normal.y, normal.z, light);
    }

    private void vertex(VertexConsumer vertexConsumer, float x, float y, float z, float u, float v, float normalX, float normalY, float normalZ, int light) {
        vertexConsumer.vertex(positionCache, x, y, z).color(255, 255, 255, 255).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normalCache, normalX, normalY, normalZ).next();
    }
}
