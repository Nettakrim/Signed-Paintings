package com.nettakrim.signed_paintings;

import net.minecraft.util.Identifier;

public class PaintingInfo {
    public Cuboid cuboid;
    private ImageData image;
    private Identifier back;
    public SignType.Type signType;
    public float rotation;

    private float width;
    private float height;
    private float depth;
    private Cuboid.Centering xCentering;
    private Cuboid.Centering yCentering;

    public PaintingInfo(ImageData image, Identifier back, boolean isFront, SignType.Type signType) {
        this.image = image;
        this.back = back;
        this.signType = signType;
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
        this.width = image.width/16f;
        this.height = image.height/16f;
        while (this.width > 8 || this.height > 8) {
            this.width /= 2f;
            this.height /= 2f;
        }
        depth = 1 / 16f;
        xCentering = Cuboid.Centering.CENTER;
        yCentering = Cuboid.Centering.CENTER;
        updateCuboid();
    }

    private void updateCuboid() {
        this.cuboid =  switch (signType) {
            case WALL -> Cuboid.CreateWallCuboid(width, xCentering, height, yCentering, depth);
            case STANDING -> Cuboid.CreateFlushCuboid(width, xCentering, height, yCentering, depth);
            case HANGING, WALL_HANGING -> Cuboid.CreateCentralCuboid(width, xCentering, height, yCentering, depth);
        };
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

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
