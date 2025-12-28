package com.nettakrim.signed_paintings.rendering;

import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.util.ImageManager;

public class OverlayInfo extends ImageInfo {
    public OverlayInfo() {

    }

    public void loadOverlay(String text) {
        invalidateImage();
        String url = SignedPaintingsClient.imageManager.applyURLInferences(text);
        // with text like "Hello" it will infer "https://Hello" which wont be a valid url
        // calling getDomain on a url like this will result in "https://", so this is easy to test for
        // text like "Hello/Hi" will infer as "https://Hello/Hi", which looks enough like a url that it cant really be fixed
        // however checking if the url is valid eliminates something like "https://Hello / Hi"
        if (!SignedPaintingsClient.getDomain(url).equals("https://") && ImageManager.isValid(url)) {
            SignedPaintingsClient.imageManager.loadImage(url, this::updateImage);
        }
    }
}
