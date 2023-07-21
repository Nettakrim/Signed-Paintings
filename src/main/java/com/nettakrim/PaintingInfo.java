package com.nettakrim;

import net.minecraft.util.Identifier;

public class PaintingInfo {
    public Cuboid cuboid;
    private ImageData image;
    private Identifier back;
    public boolean isWall;
    public float rotation;

    public PaintingInfo(ImageData image, Identifier back, boolean isFront, boolean isWall) {
        this.image = image;
        this.back = back;
        this.isWall = isWall;
        this.rotation = isFront ? 0 : 180;
        updateCuboid();
    }

    public void updateImage(ImageData image) {
        this.image = image;
        updateCuboid();
    }

    public void invalidateImage() {
        this.image = null;
    }

    public boolean isReady() {
        return image != null && image.ready;
    }

    private void updateCuboid() {
        if (isWall) {
            this.cuboid = Cuboid.CreateWallCuboid(image.width / 16f, Cuboid.Centering.CENTER, image.height / 16f, Cuboid.Centering.CENTER, 1 / 16f);
        } else {
            this.cuboid = Cuboid.CreateFlushCuboid(image.width / 16f, Cuboid.Centering.CENTER, image.height / 16f, Cuboid.Centering.CENTER, 1 / 16f);
        }
    }

    public Identifier getImageIdentifier() {
        return image.identifier;
    }

    public Identifier getBackIdentifier() {
        return back;
    }
}
