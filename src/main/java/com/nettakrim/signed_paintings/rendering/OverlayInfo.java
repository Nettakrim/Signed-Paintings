package com.nettakrim.signed_paintings.rendering;

import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.util.ImageData;
import net.minecraft.util.Identifier;

public class OverlayInfo {
    public Cuboid cuboid;
    private ImageData image;

    public OverlayInfo() {

    }

    public void loadOverlay(String text) {
        invalidateImage();
        String url = SignedPaintingsClient.imageManager.applyURLInferences(text);
        SignedPaintingsClient.imageManager.loadImage(url, this::updateImage);
    }

    public void updateImage(ImageData image) {
        this.image = image;
        updateCuboid();
    }

    private void updateCuboid() {
        this.cuboid = Cuboid.CreateOverlayCuboid((float)this.image.width/this.image.height);
    }

    public void invalidateImage() {
        this.image = null;
    }

    public boolean isReady() {
        return image != null && image.ready;
    }

    public boolean needsReload() {
        return image != null && image.needsReload;
    }

    public Identifier getImageIdentifier() {
        return image.getBaseIdentifier();
    }
}
