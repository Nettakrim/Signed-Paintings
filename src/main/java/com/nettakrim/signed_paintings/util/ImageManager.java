package com.nettakrim.signed_paintings.util;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.rendering.OverlayInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ImageManager {
    private final ArrayList<URLAlias> urlAliases;
    private final HashMap<String, ImageData> urlToImageData;
    private final HashMap<String, OverlayInfo> itemNameToOverlay;
    private final HashMap<String, ArrayList<ImageDataLoadInterface>> pendingImageLoads;
    public final ArrayList<String> blockedURLs;

    public ImageManager() {
        urlAliases = new ArrayList<>();
        urlToImageData = new HashMap<>();
        itemNameToOverlay = new HashMap<>();
        pendingImageLoads = new HashMap<>();
        blockedURLs = new ArrayList<>();
    }

    //https://github.com/Patbox/Image2Map/blob/1.20/src/main/java/space/essem/image2map/Image2Map.java
    public void loadImage(String url, ImageDataLoadInterface onLoadCallback) {
        if (blockedURLs.contains(url)) return;
        ImageData imageData = urlToImageData.get(url);
        if (imageData != null) {
            if (imageData.ready) {
                onLoadCallback.onLoad(imageData);
            } else {
                pendingImageLoads.get(url).add(onLoadCallback);
            }
        } else {
            ArrayList<ImageDataLoadInterface> list = new ArrayList<>();
            list.add(onLoadCallback);
            registerImage(url, list);
        }
    }

    private void registerImage(String url, ArrayList<ImageDataLoadInterface> onLoadCallbacks) {
        pendingImageLoads.put(url, onLoadCallbacks);
        ImageData data = new ImageData();
        urlToImageData.put(url, data);
        SignedPaintingsClient.LOGGER.info("Started loading image from "+url);
        downloadImageBuffer(url).orTimeout(60, TimeUnit.SECONDS).handleAsync((image, ex) -> {
            if (image == null || ex != null) {
                urlToImageData.remove(url);
                SignedPaintingsClient.LOGGER.info("Couldn't load image "+url);
            } else {
                SignedPaintingsClient.LOGGER.info("Loaded image "+url);
                onImageLoad(image, url, data);
                for (ImageDataLoadInterface imageDataLoadInterface : onLoadCallbacks) {
                    imageDataLoadInterface.onLoad(data);
                }
                pendingImageLoads.remove(url);
            }
            return null;
        });
    }

    private void onImageLoad(BufferedImage image, String url, ImageData data) {
        Identifier identifier = new Identifier(SignedPaintingsClient.MODID, createIdentifierSafeStringFromURL(url));
        SignedPaintingsClient.LOGGER.info(identifier.toString());
        saveBufferedImageAsIdentifier(image, identifier);
        data.onImageReady(image, identifier);
        SignedPaintingsClient.LOGGER.info("Now ready to render image "+url);
    }

    private String createIdentifierSafeStringFromURL(String url) {
        StringBuilder builder = new StringBuilder();
        url = url.toLowerCase(Locale.ROOT);
        for (int i = 0; i < url.length(); i++) {
            char character = url.charAt(i);
            if (character == '_' || character == '-' || character >= 'a' && character <= 'z' || character >= '0' && character <= '9' || character == '/' || character == '.') {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    public static void saveBufferedImageAsIdentifier(BufferedImage bufferedImage, Identifier identifier) {
        //https://discord.com/channels/507304429255393322/807617488313516032/934395931380576287
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", stream);
            byte[] bytes = stream.toByteArray();

            ByteBuffer data = BufferUtils.createByteBuffer(bytes.length).put(bytes);
            data.flip();
            NativeImage img = NativeImage.read(data);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(img);

            MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, texture));
        } catch (Throwable e) {
            SignedPaintingsClient.LOGGER.info("failed to convert BufferedImage to Identifier");
        }
    }

    public static void removeImage(Identifier identifier) {
        MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().getTextureManager().destroyTexture(identifier));
    }

    public static boolean hasImage(Identifier identifier) {
        return getTexture(identifier) != null;
    }

    public static AbstractTexture getTexture(Identifier identifier) {
        return SignedPaintingsClient.client.getTextureManager().getOrDefault(identifier, null);
    }

    private CompletableFuture<BufferedImage> downloadImageBuffer(String urlStr) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (isValid(urlStr)) {
                    URL url = new URL(urlStr);
                    URLConnection connection = url.openConnection();
                    connection.setRequestProperty("User-Agent", "Signed Paintings mod");
                    connection.connect();
                    return ImageIO.read(connection.getInputStream());
                } else {
                    return null;
                }
            } catch (Throwable e) {
                return null;
            }
        });
    }

    public static boolean isValid(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void registerURLAlias(URLAlias urlAlias) {
        urlAliases.add(urlAlias);
    }

    public String applyURLInferences(String text) {
        //for some reason "https://i.imgur.com/Avp3T5M.pngabcdefg..." is a valid link, so it should be counted as just .png
        int index = text.lastIndexOf('.');
        if (index != -1) {
            String file = text.substring(index);
            String domain = text.substring(0, index);
            if (file.length() > 4 && (file.startsWith(".png") || file.startsWith(".gif") || file.startsWith(".jpg"))) {
                text = domain + file.substring(0, 4);
            }
        }

        String url = applyURLAliases(text);
        if (!url.contains("://")) {
            url = "https://"+url;
        }
        return url;
    }

    private String applyURLAliases(String text) {
        String url = text.contains("://") ? text.split("://", 2)[1] : text;
        for (URLAlias urlAlias : urlAliases) {
            url = urlAlias.tryApply(url);
        }
        return url;
    }

    public String getShortestURLInference(String url) {
        if (url.startsWith("https://")) url = url.substring(8);
        for (URLAlias urlAlias : urlAliases) {
            url = urlAlias.getShortestAlias(url);
        }
        return url;
    }

    public int reloadAll() {
        pendingImageLoads.clear();
        int i = 0;
        for (ImageData imageData : urlToImageData.values()) {
            i += imageData.reload();
        }
        urlToImageData.clear();
        itemNameToOverlay.clear();
        return i;
    }

    public int reloadUrl(String url) {
        ImageData imageData = urlToImageData.remove(url);
        if (imageData != null) {
            return imageData.reload();
        }
        return 0;
    }

    public ArrayList<ImageStatus> getAllStatus() {
        ArrayList<ImageStatus> imageStatuses = new ArrayList<>();
        urlToImageData.forEach((url, imageData) -> imageStatuses.add(imageData.getStatus().setUrl(url)));
        return imageStatuses;
    }

    public ImageStatus getUrlStatus(String url) {
        ImageData imageData = urlToImageData.get(url);
        if (imageData != null) {
            return imageData.getStatus().setUrl(url);
        }
        return null;
    }

    public int getUrlSuggestions(SuggestionsBuilder builder) {
        for (String url : urlToImageData.keySet()) {
            builder.suggest(url);
        }
        return urlToImageData.size();
    }

    public Set<String> getUrls() {
        return urlToImageData.keySet();
    }

    public OverlayInfo getOverlayInfo(String name) {
        OverlayInfo info = itemNameToOverlay.get(name);
        if (info == null || info.needsReload()) {
            info = new OverlayInfo();
            info.loadOverlay(name);
            itemNameToOverlay.put(name, info);
        }
        return info;
    }
}
