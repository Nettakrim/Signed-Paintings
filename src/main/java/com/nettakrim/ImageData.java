package com.nettakrim;

import net.minecraft.util.Identifier;

public class ImageData {
    public Identifier identifier = null;
    public boolean ready = false;

    public ImageData() {

    }

    public void onImageReady(Identifier identifier) {
        ready = true;
        this.identifier = identifier;
    }
}
