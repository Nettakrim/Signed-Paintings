package com.nettakrim.signed_paintings.rendering;

import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.util.ImageData;
import net.minecraft.resources.Identifier;

public abstract class ImageInfo {
    public Cuboid cuboid;
    protected ImageData image;

    public boolean hasTranslucency() {
        Identifier id = this.getImageIdentifier();

        if (SignedPaintingsClient.imageManager != null && id != null) {
            return SignedPaintingsClient.imageManager.hasTranslucency(id);
        }
        return false;
    }

    public Identifier getImageIdentifier() {
        return image.getBaseIdentifier();
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
}
