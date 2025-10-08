package com.nettakrim.signed_paintings.rendering;

import com.nettakrim.signed_paintings.SignedPaintingsClient;

public class OverlayInfo extends ImageInfo {
    public OverlayInfo() {

    }

    public void loadOverlay(String text) {
        invalidateImage();
        String url = SignedPaintingsClient.imageManager.applyURLInferences(text);
        SignedPaintingsClient.imageManager.loadImage(url, this::updateImage);
    }
}
