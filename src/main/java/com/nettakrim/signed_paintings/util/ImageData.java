package com.nettakrim.signed_paintings.util;

import com.nettakrim.signed_paintings.SignedPaintingsClient;
import net.minecraft.util.Identifier;
import org.joml.Vector2i;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;

public class ImageData {
    private BufferedImage baseImage;
    private Identifier baseIdentifier;
    private Identifier workingIdentifier;
    private final HashMap<Vector2i, Identifier> images;
    public boolean ready = false;
    public boolean needsReload = false;

    public int width;
    public int height;

    private int workingWidth;
    private int workingHeight;

    public ImageData() {
        this.images = new HashMap<>();
    }

    public void onImageReady(BufferedImage image, Identifier baseIdentifier) {
        this.baseImage = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.baseIdentifier = baseIdentifier;
        SignedPaintingsClient.LOGGER.info(baseIdentifier.toString());
        this.workingIdentifier = baseIdentifier.withSuffixedPath("_working");
        Vector2i d = new Vector2i(width, height);
        images.put(d, baseIdentifier);
        this.ready = true;
    }

    public Identifier getBaseIdentifier() {
        return baseIdentifier;
    }

    public Identifier getIdentifier(int width, int height, boolean working) {
        Vector2i resolution = new Vector2i(width, height);
        Identifier identifier = images.get(resolution);
        if (identifier != null) return identifier;

        if (working) {
            if (width != workingWidth || height != workingHeight) {
                workingWidth = width;
                workingHeight = height;
                ImageManager.saveBufferedImageAsIdentifier(scaleImage(baseImage, width, height), workingIdentifier);
            }
            return workingIdentifier;
        } else {
            Identifier newIdentifier = baseIdentifier.withSuffixedPath("_"+width+"x"+height);
            ImageManager.saveBufferedImageAsIdentifier(scaleImage(baseImage, width, height), newIdentifier);
            images.put(resolution, newIdentifier);
            return newIdentifier;
        }
    }

    private BufferedImage scaleImage(BufferedImage referenceImage, int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, referenceImage.getType());
        Graphics2D graphics2D = resizedImage.createGraphics();
        // refer to https://docs.oracle.com/javase/tutorial/2d/advanced/quality.html
        //graphics2D.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        graphics2D.drawImage(referenceImage, 0, 0, width, height, null);
        graphics2D.dispose();
        return resizedImage;
    }

    public int clear() {
        ready = false;
        int i = 0;
        if (ImageManager.hasImage(workingIdentifier)) i++;
        ImageManager.removeImage(baseIdentifier);
        ImageManager.removeImage(workingIdentifier);
        for (Identifier identifier : images.values()) {
            ImageManager.removeImage(identifier);
            i++;
        }
        needsReload = true;
        images.clear();
        return i;
    }
}
