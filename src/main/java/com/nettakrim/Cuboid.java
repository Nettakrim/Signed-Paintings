package com.nettakrim;

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

    public static Cuboid CreateWallCuboid(float xSize, Centering xCentering, float ySize, Centering yCentering, float zSize) {
        return new Cuboid(xSize, ySize, zSize, getOffsetFromCentering(xSize, xCentering), getOffsetFromCentering(ySize, yCentering), -0.5f + (zSize/2));
    }

    public static Cuboid CreateFlushCuboid(float xSize, Centering xCentering, float ySize, Centering yCentering, float zSize) {
        return new Cuboid(xSize, ySize, zSize, getOffsetFromCentering(xSize, xCentering), getOffsetFromCentering(ySize, yCentering), 0.5f - (zSize/2));
    }

    private static float getOffsetFromCentering(float size, Centering centering) {
        return switch (centering) {
            case MIN -> (size-1)/2;
            case CENTER -> 0;
            case MAX -> (1-size)/2;
        };
    }

    public enum Centering {
        MIN,
        CENTER,
        MAX
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
            Vector3f cornerBL = adjustVertex(new Vector3f(-0.5f, -0.5f, 0.5F), rotation);
            Vector3f cornerBR = adjustVertex(new Vector3f( 0.5f, -0.5f, 0.5F), rotation);
            Vector3f cornerTR = adjustVertex(new Vector3f( 0.5f,  0.5f, 0.5F), rotation);
            Vector3f cornerTL = adjustVertex(new Vector3f(-0.5f,  0.5f, 0.5F), rotation);
            vertexFromVector(vertexConsumer, cornerBL, minU, maxV, normal, light);
            vertexFromVector(vertexConsumer, cornerBR, maxU, maxV, normal, light);
            vertexFromVector(vertexConsumer, cornerTR, maxU, minV, normal, light);
            vertexFromVector(vertexConsumer, cornerTL, minU, minV, normal, light);
            return;
        }

        Vector3f relevantSize = new Vector3f(size);
        rotation.transform(relevantSize);
        relevantSize.absolute();

        for (float minX = 0; minX < relevantSize.x; minX++) {
            for (float minY = 0; minY < relevantSize.y; minY++) {

                float maxX = Math.min(minX+1, relevantSize.x);
                float maxY = Math.min(minY+1, relevantSize.y);

                float xDelta = maxX-minX;
                float yDelta = maxY-minY;

                float scaledMinX = (minX/relevantSize.x)-0.5f;
                float scaledMaxX = (maxX/relevantSize.x)-0.5f;
                float scaledMinY = (minY/relevantSize.y)-0.5f;
                float scaledMaxY = (maxY/relevantSize.y)-0.5f;

                Vector3f cornerBL = adjustVertex(new Vector3f(scaledMinX, scaledMinY, 0.5F), rotation);
                Vector3f cornerBR = adjustVertex(new Vector3f(scaledMaxX, scaledMinY, 0.5F), rotation);
                Vector3f cornerTR = adjustVertex(new Vector3f(scaledMaxX, scaledMaxY, 0.5F), rotation);
                Vector3f cornerTL = adjustVertex(new Vector3f(scaledMinX, scaledMaxY, 0.5F), rotation);

                float newMaxU = minU+((maxU-minU)*xDelta);
                float newMaxV = minV+((maxV-minV)*yDelta);

                vertexFromVector(vertexConsumer, cornerBL, minU,    newMaxV, normal, light);
                vertexFromVector(vertexConsumer, cornerBR, newMaxU, newMaxV, normal, light);
                vertexFromVector(vertexConsumer, cornerTR, newMaxU, minV,    normal, light);
                vertexFromVector(vertexConsumer, cornerTL, minU,    minV,    normal, light);
            }
        }
    }

    private void vertexFromVector(VertexConsumer vertexConsumer, Vector3f vertexPos, float u, float v, Vector3f normal, int light) {
        this.vertex(vertexConsumer, vertexPos.x, vertexPos.y, vertexPos.z, u, v, normal.x, normal.y, normal.z, light);
    }

    private void vertex(VertexConsumer vertexConsumer, float x, float y, float z, float u, float v, float normalX, float normalY, float normalZ, int light) {
        vertexConsumer.vertex(positionCache, x, y, z).color(255, 255, 255, 255).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normalCache, normalX, normalY, normalZ).next();
    }
}
