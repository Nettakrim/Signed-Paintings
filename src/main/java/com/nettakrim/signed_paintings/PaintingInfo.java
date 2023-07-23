package com.nettakrim.signed_paintings;

import net.minecraft.util.Identifier;

public class PaintingInfo {
    public Cuboid cuboid;
    private ImageData image;
    private Identifier back;
    public boolean isWall;
    public float rotation;

    private float width;
    private float height;
    private float depth;
    private Cuboid.Centering xCentering;
    private Cuboid.Centering yCentering;

    public PaintingInfo(ImageData image, Identifier back, boolean isFront, boolean isWall) {
        this.image = image;
        this.back = back;
        this.isWall = isWall;
        this.rotation = isFront ? 0 : 180;
        resetCuboid();
    }

    public void updateImage(ImageData image) {
        this.image = image;
        resetCuboid();
    }

    public void invalidateImage() {
        this.image = null;
    }

    public boolean isReady() {
        return image != null && image.ready;
    }

    private void resetCuboid() {
        width = image.width / 16f;
        height = image.height / 16f;
        depth = 1 / 16f;
        xCentering = Cuboid.Centering.CENTER;
        yCentering = Cuboid.Centering.CENTER;
        updateCuboid();
    }

    private void updateCuboid() {
        if (isWall) {
            this.cuboid = Cuboid.CreateWallCuboid(width, xCentering, height, yCentering, depth);
        } else {
            this.cuboid = Cuboid.CreateFlushCuboid(width, xCentering, height, yCentering, depth);
        }
    }

    public void updateCuboidCentering(Cuboid.Centering xCentering, Cuboid.Centering yCentering) {
        this.xCentering = xCentering;
        this.yCentering = yCentering;
        updateCuboid();
    }

    public void updateCuboidSize(float xSize, float ySize) {
        this.width = xSize;
        this.height = ySize;
        updateCuboid();
    }

    public void setBackIdentifier(Identifier back) {
        this.back = back;
    }

    public Identifier getImageIdentifier() {
        return image.identifier;
    }

    public Identifier getBackIdentifier() {
        return back;
    }
}
