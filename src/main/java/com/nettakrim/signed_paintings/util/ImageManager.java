package com.nettakrim.signed_paintings.util;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.gui.UIHelper;
import com.nettakrim.signed_paintings.mixin.TextureManagerAccessor;
import com.nettakrim.signed_paintings.rendering.OverlayInfo;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.net.URI;
import java.util.Map;
import java.util.HashMap;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ImageManager {
    private final File data;
    private final ArrayList<URLAlias> urlAliases;
    private final Map<Identifier, Boolean> transparencyCache = new HashMap<>();
    private final HashMap<String, ImageData> urlToImageData;
    private final HashMap<String, OverlayInfo> itemNameToOverlay;
    private final HashMap<String, ArrayList<ImageDataLoadInterface>> pendingImageLoads;
    public final ArrayList<String> blockedURLs;
    public final ArrayList<String> allowedDomains;
    public final ArrayList<String> imgurApiKeys;
    public boolean autoBlockNew = false;

    private boolean changesMade = false;
    public boolean hasPartialTransparency(Identifier id) {
        return transparencyCache.getOrDefault(id, false);
    }

    private void checkAndCacheTransparency(Identifier id, BufferedImage bufferedImage) {
        if (bufferedImage == null) {
            transparencyCache.put(id, false);
            SignedPaintingsClient.info("Cannot check transparency for null BufferedImage: " + id, false);
            return;
        }

        if (transparencyCache.containsKey(id)) {
            return;
        }

        boolean hasPartial = false;
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        try {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int color = bufferedImage.getRGB(x, y);
                    int alpha = (color >> 24) & 0xFF;

                    if (alpha > 0 && alpha < 255) {
                        hasPartial = true;
                        break; 
                    }
                }
                if (hasPartial) {
                    break; 
                }
            }
        } catch (Exception e) {
            SignedPaintingsClient.info("Error checking transparency for " + id + ": " + e.getMessage(), true);
        }

        transparencyCache.put(id, hasPartial);
    }

    public ImageManager() {
        urlAliases = new ArrayList<>();
        urlToImageData = new HashMap<>();
        itemNameToOverlay = new HashMap<>();
        pendingImageLoads = new HashMap<>();
        blockedURLs = new ArrayList<>();
        allowedDomains = new ArrayList<>();
        imgurApiKeys = new ArrayList<>();

        data = FabricLoader.getInstance().getConfigDir().resolve("signed_paintings.txt").toFile();
        try {
            if (data.exists()) {
                Scanner scanner = new Scanner(data);
                if (scanner.hasNextLine()) scanner.nextLine();
                int phase = 0;
                int lines = 0;
                while (scanner.hasNextLine()) {
                    String s = scanner.nextLine();
                    if (s.startsWith("-")) {
                        phase++;
                        lines = 0;
                        continue;
                    } else {
                        lines++;
                    }

                    if (phase == 0) {
                        blockedURLs.add(s);
                    } else if (phase == 1) {
                        allowedDomains.add(s);
                    } else if (phase == 2) {
                        SignedPaintingsClient.loggingEnabled = s.equals("true");
                    } else if (phase == 3) {
                        boolean active = s.equals("true");
                        switch (lines) {
                            case 0:
                                SignedPaintingsClient.renderSigns = active;
                            case 1:
                                SignedPaintingsClient.renderBanners = active;
                            case 2:
                                SignedPaintingsClient.renderShields = active;
                            case 3:
                                SignedPaintingsClient.reduceCulling = active;
                            case 4:
                                UIHelper.setBackgroundEnabled(active);
                        }
                    } else if (phase == 4) {
                        imgurApiKeys.add(s);
                    }
                }
                scanner.close();
            } else {
                changesMade = true;
            }
        } catch (IOException e) {
            SignedPaintingsClient.info("Failed to load data", true);
        }

        if (imgurApiKeys.isEmpty()) {
            imgurApiKeys.add("274478faed23e08");
            imgurApiKeys.add("0a74b33065e56a7");
            imgurApiKeys.add("2b12ffa92e72e63");
            imgurApiKeys.add("c1802a39166b9d0");
        }
    }

    public void save() {
        if (data.exists() && !changesMade) return;
        try {
            if (!data.exists()) data.createNewFile();
            FileWriter writer = new FileWriter(data);

            StringBuilder s = new StringBuilder("- Blocked Painting URLs -");
            for (String url : blockedURLs) {
                s.append("\n").append(url);
            }

            s.append("\n- Allowed URL Domains -");
            for (String url : allowedDomains) {
                s.append("\n").append(url);
            }

            s.append("\n- Detailed Logs -");
            s.append("\n").append(SignedPaintingsClient.loggingEnabled ? "true" : "false");

            s.append("\n- Rendering Toggles -");
            s.append("\n").append(SignedPaintingsClient.renderSigns ? "true" : "false");
            s.append("\n").append(SignedPaintingsClient.renderBanners ? "true" : "false");
            s.append("\n").append(SignedPaintingsClient.renderShields ? "true" : "false");
            s.append("\n").append(SignedPaintingsClient.reduceCulling ? "true" : "false");
            s.append("\n").append(UIHelper.isBackgroundEnabled() ? "true" : "false");

            s.append("\n- Imgur API Keys (get your own at https://api.imgur.com/oauth2/addclient) -");
            for (String key : imgurApiKeys) {
                s.append("\n").append(key);
            }

            writer.write(s.toString());
            writer.close();
            changesMade = false;
        } catch (IOException e) {
            SignedPaintingsClient.info("Failed to save data", true);
        }
    }

    //https://github.com/Patbox/Image2Map/blob/1.20/src/main/java/space/essem/image2map/Image2Map.java
    public void loadImage(String url, ImageDataLoadInterface onLoadCallback) {
        if (url.equals("https://")) return;
        ImageData imageData = urlToImageData.get(url);
        boolean blocked = blockedURLs.contains(url) || domainBlocked(url);

        if (!blocked && autoBlockNew) {
            SignedPaintingsClient.sayRaw(
                Text.translatable(SignedPaintingsClient.MODID+".commands.block.notify.base",
                    Text.translatable(SignedPaintingsClient.MODID+".commands.block.notify.text", url)
                        .setStyle(Style.EMPTY.withColor(SignedPaintingsClient.textColor).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/paintings:block remove "+url)))
                    )
                    .setStyle(Style.EMPTY.withColor(SignedPaintingsClient.nameTextColor)
                )
            );
            blockedURLs.add(url);
            blocked = true;
        }

        if (imageData != null) {
            if (imageData.ready || blocked) {
                onLoadCallback.onLoad(imageData);
            } else {
                pendingImageLoads.get(url).add(onLoadCallback);
            }
        } else {
            ArrayList<ImageDataLoadInterface> list = new ArrayList<>();
            list.add(onLoadCallback);
            registerImage(url, list, blocked);
        }
    }

    private void registerImage(String url, ArrayList<ImageDataLoadInterface> onLoadCallbacks, boolean blocked) {
        ImageData data = new ImageData();
        urlToImageData.put(url, data);
        if (blocked) {
            for (ImageDataLoadInterface imageDataLoadInterface : onLoadCallbacks) {
                imageDataLoadInterface.onLoad(data);
            }
            return;
        }
        pendingImageLoads.put(url, onLoadCallbacks);
        SignedPaintingsClient.info("Started loading image from "+url, false);
        downloadImageBuffer(url).orTimeout(60, TimeUnit.SECONDS).handleAsync((image, ex) -> {
            if (image == null || ex != null) {
                urlToImageData.remove(url);
                SignedPaintingsClient.info("Couldn't load image "+url+"\n"+ex.toString(), true);
            } else {
                SignedPaintingsClient.info("Loaded image "+url, false);
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
        Identifier identifier = Identifier.of(SignedPaintingsClient.MODID, createIdentifierSafeStringFromURL(url));
        saveBufferedImageAsIdentifier(image, identifier);
        data.onImageReady(image, identifier);
        SignedPaintingsClient.info("Ready to render Image "+url, true);
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
        // https://discord.com/channels/507304429255393322/807617488313516032/934395931380576287
        NativeImage img = null;
        try {
            if (SignedPaintingsClient.imageManager != null) {
                 SignedPaintingsClient.imageManager.checkAndCacheTransparency(identifier, bufferedImage);
            } else {
                 SignedPaintingsClient.info("ImageManager instance not available for transparency check: " + identifier, true);
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", stream);
            byte[] bytes = stream.toByteArray();

            ByteBuffer data = BufferUtils.createByteBuffer(bytes.length).put(bytes);
            data.flip();
            img = NativeImage.read(data);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(img);

            MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, texture));

        } catch (Throwable e) {
            SignedPaintingsClient.info("Failed to convert/register BufferedImage for identifier \"" + identifier + "\": " + e.getMessage(), true);
            if (img != null) {
                MinecraftClient.getInstance().execute(img::close);
            }
            if (SignedPaintingsClient.imageManager != null) {
                 SignedPaintingsClient.imageManager.transparencyCache.put(identifier, false);
            }
        }
    }


    public static void removeImage(Identifier identifier) {
        MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().getTextureManager().destroyTexture(identifier));
    }

    public static boolean hasImage(Identifier identifier) {
        return getTexture(identifier) != null;
    }

    public static AbstractTexture getTexture(Identifier identifier) {
        return ((TextureManagerAccessor)SignedPaintingsClient.client.getTextureManager()).getTextures().get(identifier);
    }

    private CompletableFuture<BufferedImage> downloadImageBuffer(String urlStr) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (isValid(urlStr)) {
                    URLConnection connection = URI.create(urlStr).toURL().openConnection();
                    connection.setRequestProperty("User-Agent", "Signed Paintings mod");
                    connection.setRequestProperty("Sec-Fetch-Site", "same-site");
                    connection.setRequestProperty("Referer", "https://imgur.com/");
                    connection.connect();
                    return ImageIO.read(connection.getInputStream());
                } else {
                    SignedPaintingsClient.info("invalid url string "+urlStr, false);
                    return null;
                }
            } catch (Throwable e) {
                SignedPaintingsClient.info("error downloading image: "+e, true);
                return null;
            }
        });
    }

    public static boolean isValid(String url) {
        try {
            //noinspection ResultOfMethodCallIgnored (throws for malformed urls)
            URI.create(url).toURL();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void registerURLAlias(URLAlias urlAlias) {
        urlAliases.add(urlAlias);
    }

    public void registerAllowedDomain(String url) {
        if (allowedDomains.contains(url)) return;
        allowedDomains.add(url);
    }

    public boolean domainBlocked(String url) {
        for (String allowed : allowedDomains) {
            if (url.startsWith(allowed)) {
                return false;
            }
        }
        return true;
    }

    public String applyURLInferences(String text) {
        if (text.startsWith("ftp://")) {
            return text;
        }

        //for some reason "https://i.imgur.com/Avp3T5M.pngabcdefg..." is a valid link, so it should be counted as just .png
        if (text.contains("i.imgur.com")) {
            int index = text.lastIndexOf('.');
            if (index != -1) {
                String file = text.substring(index);
                String domain = text.substring(0, index);
                if (file.length() > 4 && (file.startsWith(".png") || file.startsWith(".gif") || file.startsWith(".jpg"))) {
                    text = domain + file.substring(0, 4);
                }
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
        for (Map.Entry<String, ImageData> entry : urlToImageData.entrySet()) {
            if (entry.getValue().ready) {
                builder.suggest(entry.getKey());
            }
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

    public void makeChange() {
        changesMade = true;
    }
}
