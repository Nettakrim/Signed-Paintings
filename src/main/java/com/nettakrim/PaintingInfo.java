package com.nettakrim;

import net.minecraft.util.Identifier;

public class PaintingInfo {
    public Cuboid cuboid;
    public ImageData image;
    public Identifier back;

    public PaintingInfo(Cuboid cuboid, ImageData image, Identifier back) {
        this.cuboid = cuboid;
        this.image = image;
        this.back = back;
    }
}
