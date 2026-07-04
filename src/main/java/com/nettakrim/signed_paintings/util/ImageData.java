package com.nettakrim.signed_paintings.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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

    private final Map<Identifier, Set<BlockEntity>> sectionUpdateListeners = new ConcurrentHashMap<>();

    public void addSectionUpdateListener(Identifier identifier, BlockEntity blockEntity) {
        if (identifier == null) return;

        sectionUpdateListeners
                .computeIfAbsent(identifier, _ -> Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>())))
                .add(blockEntity);
    }

    public void pruneSectionUpdateListeners() {
        sectionUpdateListeners.values().removeIf(Set::isEmpty);
    }

    public void removeSectionUpdateListener(Identifier identifier, BlockEntity blockEntity) {
        if (identifier == null) return;

        if (sectionUpdateListeners.containsKey(identifier)) {
            var set = sectionUpdateListeners.get(identifier);
            set.remove(blockEntity);
            if (set.isEmpty()) {
                sectionUpdateListeners.remove(identifier);
            }
        }
    }

    public void notifySectionUpdateListeners(Identifier identifier) {
        if (identifier == null) return;
        Set<BlockEntity> listeners = sectionUpdateListeners.get(identifier);
        if (listeners == null) return;

        synchronized (listeners) {
            for (BlockEntity blockEntity : listeners) {
                SignedPaintingsClient.client.execute(() -> {
                    if (blockEntity.isRemoved()) return;
                    BlockPos pos = blockEntity.getBlockPos();
                    BlockState state = blockEntity.getLevel().getBlockState(pos);
                    blockEntity.getLevel().sendBlockUpdated(pos, state, state, 3);
                });
            }
        }
    }

    public void onImageReady(BufferedImage image, Identifier baseIdentifier) {
        this.baseImage = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.baseIdentifier = baseIdentifier;
        this.workingIdentifier = baseIdentifier.withSuffix("_working");
        this.ready = true;
        notifySectionUpdateListeners(baseIdentifier);
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
                ImageManager.saveBufferedImageAsIdentifier(ImageManager.scaleImage(baseImage, width, height), workingIdentifier);
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
                bufferedImage = ImageManager.scaleImage(baseImage, width, height);
            }

            if (identifier == null)
                return null;

            if (!loadingImages.add(identifier))
                return identifier;

            ImageManager.saveBufferedImageAsIdentifierAsync(bufferedImage, identifier).handleAsync((v, e) -> {
                if (e != null) {
                    loadingImages.remove(identifier);
                    return null;
                }
                images.put(resolution, new VariantData(identifier));
                loadingImages.remove(identifier);
                notifySectionUpdateListeners(identifier);

                return null;
            });

            return identifier;
        }
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
