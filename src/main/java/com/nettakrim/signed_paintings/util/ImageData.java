package com.nettakrim.signed_paintings.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import org.joml.Vector2i;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public class ImageData {
    private BufferedImage baseImage;
    private Identifier baseIdentifier;
    private Identifier workingIdentifier;
    private final ConcurrentHashMap<Vector2i, VariantData> images = new ConcurrentHashMap<>();
    private final Set<Identifier> loadingImages = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public boolean ready = false;
    public boolean needsReload = false;

    public int width;
    public int height;

    private int workingWidth;
    private int workingHeight;
    private int workingRenderTime = -1;

    private int expiredAllAt = -1;


    public ImageData() {
    }

    public void onImageReady(BufferedImage image, Identifier baseIdentifier) {
        this.baseImage = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.baseIdentifier = baseIdentifier;
        this.workingIdentifier = baseIdentifier.withSuffix("_working");
        this.ready = true;
    }

    public Identifier getBaseIdentifier() {
        return getIdentifier(width, height, false);
    }

    public Identifier getIdentifier(int width, int height, boolean working) {
        expiredAllAt = -1;

        Vector2i resolution = new Vector2i(width, height);
        VariantData variantData = images.get(resolution);
        if (variantData != null) {
            variantData.renderTime = SignedPaintingsClient.imageManager.renderTime;
            return variantData.identifier;
        }

        if (working) {
            workingRenderTime = SignedPaintingsClient.imageManager.renderTime;
            if (width != workingWidth || height != workingHeight) {
                workingWidth = width;
                workingHeight = height;
                ImageManager.saveBufferedImageAsIdentifier(scaleImage(baseImage, width, height), workingIdentifier);
            }

            return workingIdentifier;
        } else {
            Identifier identifier;
            BufferedImage bufferedImage;

            if (width == this.width && height == this.height) {
                identifier = baseIdentifier;
                bufferedImage = baseImage;
            } else {
                identifier = baseIdentifier.withSuffix("_"+width+"x"+height);
                bufferedImage = scaleImage(baseImage, width, height);
            }

            if (identifier == null)
                return null;

            if (loadingImages.contains(identifier))
                return identifier;

            loadingImages.add(identifier);

            ImageManager.saveBufferedImageAsIdentifierAsync(bufferedImage, identifier).handleAsync((v, e) -> {
                if (e != null)
                {
                    loadingImages.remove(identifier);
                    return null;
                }
                images.put(resolution, new VariantData(identifier));
                loadingImages.remove(identifier);

                return null;
            });

            return identifier;
        }
    }

    private BufferedImage scaleImage(BufferedImage referenceImage, int width, int height) {
        width = Math.max(width, 1);
        height = Math.max(height, 1);
        BufferedImage resizedImage = new BufferedImage(width, height, referenceImage.getType());
        Graphics2D graphics2D = resizedImage.createGraphics();
        // refer to https://docs.oracle.com/javase/tutorial/2d/advanced/quality.html
        //graphics2D.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        graphics2D.drawImage(referenceImage, 0, 0, width, height, null);
        graphics2D.dispose();
        return resizedImage;
    }

    public int reload() {
        ready = false;

        int i = 0;
        if (ImageManager.hasImage(workingIdentifier)) {
            i++;
            ImageManager.removeImage(workingIdentifier);
        }
        for (VariantData variantData : images.values()) {
            ImageManager.removeImage(variantData.identifier);
            i++;
        }
        baseImage = null;

        needsReload = true;
        images.clear();
        return i;
    }

    public ImageStatus getStatus() {
        ImageStatus imageStatus = new ImageStatus();
        images.forEach((key, value) -> imageStatus.addResolution(key, getBytes(Objects.requireNonNull(((DynamicTexture) ImageManager.getTexture(value.identifier)).getPixels())), value.identifier != baseIdentifier));
        imageStatus.ready = ready;
        return imageStatus;
    }

    private long getBytes(NativeImage image) {
        long bytesPerPixel = image.format().components();
        return image.getWidth()*image.getHeight()*bytesPerPixel;
    }

    public boolean checkRenderTime(int expireVram, int expireFully) {
        if (!ready) {
            return false;
        }

        for (Iterator<VariantData> iterator = images.values().iterator(); iterator.hasNext();) {
            VariantData variantData = iterator.next();
            if (variantData.renderTime < expireVram) {
                SignedPaintingsClient.info("removing expired image variant "+variantData.identifier, false);
                ImageManager.removeImage(variantData.identifier);
                iterator.remove();
            }
        }

        if (workingRenderTime != -1 && workingRenderTime < expireVram) {
            SignedPaintingsClient.info("removing expired image variant "+workingIdentifier, false);
            ImageManager.removeImage(workingIdentifier);
            workingRenderTime = -1;
        }


        if (images.isEmpty() && workingRenderTime == -1) {
            if (expiredAllAt == -1) {
                expiredAllAt = SignedPaintingsClient.imageManager.renderTime;
            }

            if (expiredAllAt < expireFully) {
                SignedPaintingsClient.info("reloading fully expired image " + baseIdentifier, false);
                reload();
                return true;
            }
        }
        return false;
    }

    private static class VariantData {
        public Identifier identifier;
        public int renderTime;

        public VariantData(Identifier identifier) {
            this.identifier = identifier;
            renderTime = SignedPaintingsClient.imageManager.renderTime;
        }
    }
}
