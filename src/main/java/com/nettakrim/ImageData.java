package com.nettakrim;

import net.minecraft.util.Identifier;

import java.awt.image.BufferedImage;

public class ImageData {
    public Identifier identifier = null;
    public boolean ready = false;

    public int width;
    public int height;

    public ImageData() {

    }

    public void onImageReady(Identifier identifier, BufferedImage image) {
        this.identifier = identifier;
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.ready = true;
    }
}
