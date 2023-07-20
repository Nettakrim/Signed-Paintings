package com.nettakrim;

import net.minecraft.util.Identifier;

public class PaintingInfo {
    public Cuboid cuboid;
    public ImageData image;
    public Identifier back;

    public PaintingInfo(Cuboid cuboid, String url, Identifier back) {
        this.cuboid = cuboid;
        this.image = SignedPaintingsClient.imageManager.getImageData(url);
        this.back = back;
    }

    public void updateImage(String url) {
        this.image = SignedPaintingsClient.imageManager.getImageData(url);
    }

    public boolean isReady() {
        if (image == null) return false;
        return image.ready;
    }
}
