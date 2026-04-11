package com.nettakrim.signed_paintings.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import com.nettakrim.signed_paintings.gui.UIHelper;
import com.nettakrim.signed_paintings.mixin.TextureManagerAccessor;
import com.nettakrim.signed_paintings.rendering.OverlayInfo;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.PngInfo;
import java.net.URI;
import java.nio.IntBuffer;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ImageManager {
    private final String dataHeader = "https://modrinth.com/mod/signed-paintings config v";
    private final int dataVersion = 2;
    private final File data;

    private final ArrayList<URLAlias> urlAliases;
    private final Map<Identifier, Boolean> translucencyCache = new HashMap<>();
    private final HashMap<String, ImageData> urlToImageData;
    private final HashMap<String, OverlayInfo> itemNameToOverlay;
    private final HashMap<String, ArrayList<ImageDataLoadInterface>> pendingImageLoads;
    public final ArrayList<String> blockedURLs;
    public final Set<String> trustedDomains;
    public final Set<String> blockPromptedDomains;
    public boolean autoBlockNew = false;
    public int renderTime = 0;

    private static final Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private static final Executor singleThreadExecutor = Executors.newSingleThreadExecutor();

    private boolean changesMade = false;
    public boolean hasTranslucency(Identifier id) {
        return translucencyCache.getOrDefault(id, false);
    }

    private void checkAndCacheTranslucency(Identifier id, BufferedImage bufferedImage) {
        if (bufferedImage == null) {
            translucencyCache.put(id, false);
            SignedPaintingsClient.info("Cannot check transparency for null BufferedImage: " + id, false);
            return;
        }

        if (translucencyCache.containsKey(id)) {
            return;
        }

        boolean hasTranslucency = false;
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        try {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int color = bufferedImage.getRGB(x, y);
                    int alpha = (color >> 24) & 0xFF;

                    if (alpha > 0 && alpha < 255) {
                        hasTranslucency = true;
                        break;
                    }
                }
                if (hasTranslucency) {
                    break;
                }
            }
        } catch (Exception e) {
            SignedPaintingsClient.info("Error checking transparency for " + id + ": " + e.getMessage(), true);
        }

        translucencyCache.put(id, hasTranslucency);
    }

    public ImageManager() {
        urlAliases = new ArrayList<>();
        urlToImageData = new HashMap<>();
        itemNameToOverlay = new HashMap<>();
        pendingImageLoads = new HashMap<>();
        blockedURLs = new ArrayList<>();
        trustedDomains = new HashSet<>();
        blockPromptedDomains = new HashSet<>();

        data = FabricLoader.getInstance().getConfigDir().resolve("signed_paintings.txt").toFile();
        try {
            if (data.exists()) {
                Scanner scanner = new Scanner(data);

                int phase = 0;
                int lines = 0;

                if (scanner.hasNextLine()) {
                    String version = scanner.nextLine();
                    if (version.startsWith(dataHeader)) {
                        // next line will be the first phase marker, which will move it to phase 0, for consistency with old format
                        phase = -1;
                    } else {
                        // loading older format without version header, user needs to be notified about upload removal
                        SignedPaintingsClient.sayText(Component.translatable(SignedPaintingsClient.MODID+".upload_change_notification").setStyle(SignedPaintingsClient.getUrlButton("https://github.com/Nettakrim/Signed-Paintings/blob/fixes/upload_removal.md")));
                        makeChange();
                    }
                }

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
                        trustedDomains.add(s);
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
                        String[] parts = s.split(" ", 3);
                        if (parts.length == 3) {
                            urlAliases.add(new URLAlias(parts[0], parts[2].split(" "), parts[1]));
                        } else {
                            SignedPaintingsClient.info("invalid url alias: \""+s+"\"", true);
                        }
                    }
                }
                scanner.close();
            } else {
                changesMade = true;
            }
        } catch (IOException e) {
            SignedPaintingsClient.info("Failed to load data", true);
        }

        if (urlAliases.isEmpty()) {
            registerURLAlias(new URLAlias("https://i.imgur.com/", new String[]{"i.imgur.com/", "imgur:"}, ".png"));
            registerURLAlias(new URLAlias("https://iili.io/", new String[]{"freeimage.host/i/", "iili:"}, ".png"));
            makeChange();
        }
        if (trustedDomains.isEmpty()) {
            trustDomain("https://i.imgur.com/");
            trustDomain("https://iili.io/");
            trustDomain("https://i.ibb.co/");
            trustDomain("https://upload.wikimedia.org/");
            trustDomain("https://web.archive.org/");
            makeChange();
        }
    }

    public void save() {
        if (!changesMade) return;
        try {
            if (!data.exists()) {
                data.createNewFile();
            }
            FileWriter writer = new FileWriter(data);

            StringBuilder s = new StringBuilder(dataHeader+dataVersion);
            s.append("\n- Blocked Painting URLs -");
            for (String url : blockedURLs) {
                s.append("\n").append(url);
            }

            s.append("\n- Trusted URL Domains -");
            for (String url : trustedDomains) {
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

            s.append("\n- Aliases (list separated by spaces, first item is what url it will use, second item should be left as .png, then theres a list of aliases. to redirect, put your redirect host as the first item, and the put the original host (without the https://) in the alias list) -");
            for (URLAlias alias : urlAliases) {
                s.append("\n").append(alias.save());
            }

            writer.write(s.toString());
            writer.close();
            changesMade = false;
        } catch (IOException e) {
            SignedPaintingsClient.info("Failed to save data\n"+e.getMessage()+"\n"+Arrays.toString(e.getStackTrace()), true);
        }
    }

    //https://github.com/Patbox/Image2Map/blob/1.20/src/main/java/space/essem/image2map/Image2Map.java
    public void loadImage(String url, ImageDataLoadInterface onLoadCallback) {
        if (url.equals("https://")) return;
        ImageData imageData = urlToImageData.get(url);

        boolean blocked = blockedURLs.contains(url);

        if (!blocked && domainBlocked(url)) {
            String domain = SignedPaintingsClient.getDomain(url);
            SignedPaintingsClient.info("Prompting domain trust for '"+ domain +"' from url '"+url+"'", false);

            if (blockPromptedDomains.add(domain)) {
                ClickEvent clickEvent = new ClickEvent.SuggestCommand("/paintings:domain trust " + domain);

                SignedPaintingsClient.sayRaw(
                        Component.translatable(SignedPaintingsClient.MODID + ".commands.domain.notify",
                                Component.translatable(SignedPaintingsClient.MODID + ".commands.domain.notify.click")
                                        .setStyle(Style.EMPTY.withColor(SignedPaintingsClient.nameTextColor).withClickEvent(clickEvent)
                                        ),
                                domain
                        ).setStyle(Style.EMPTY.withColor(SignedPaintingsClient.textColor).withClickEvent(clickEvent))
                );
            }

            blocked = true;
        }


        if (!blocked && autoBlockNew) {
            SignedPaintingsClient.sayRaw(
                Component.translatable(SignedPaintingsClient.MODID+".commands.block.notify.base",
                    Component.translatable(SignedPaintingsClient.MODID+".commands.block.notify.text", url)
                            .setStyle(Style.EMPTY.withColor(SignedPaintingsClient.textColor).withClickEvent(new ClickEvent.SuggestCommand("/paintings:block remove "+url)))
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
            } else if (pendingImageLoads.containsKey(url)) {
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
        Identifier identifier = Identifier.fromNamespaceAndPath(SignedPaintingsClient.MODID, createIdentifierSafeStringFromURL(url));
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
        if (SignedPaintingsClient.imageManager != null) {
            SignedPaintingsClient.imageManager.checkAndCacheTranslucency(identifier, bufferedImage);
        } else {
            SignedPaintingsClient.info("ImageManager instance not available for transparency check: " + identifier, true);
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try {
            ImageIO.write(bufferedImage, "png", stream);
        } catch (IOException e) {
            SignedPaintingsClient.info("Failed to convert/register BufferedImage for identifier \"" + identifier + "\": " + e.getMessage(), true);
            if (SignedPaintingsClient.imageManager != null) {
                SignedPaintingsClient.imageManager.translucencyCache.put(identifier, false);
            }
            return;
        }

        byte[] bytes = stream.toByteArray();

        ByteBuffer data = BufferUtils.createByteBuffer(bytes.length).put(bytes);
        data.flip();

        try {
            PngInfo.validateHeader(data);

            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                IntBuffer xBuffer = memoryStack.mallocInt(1);
                IntBuffer yBuffer = memoryStack.mallocInt(1);
                IntBuffer channelBuffer = memoryStack.mallocInt(1);
                ByteBuffer byteBuffer = STBImage.stbi_load_from_memory(data, xBuffer, yBuffer, channelBuffer, 4);

                AtomicReference<NativeImage> nativeImage = new AtomicReference<>();
                Minecraft.getInstance().executeBlocking(() ->
                        nativeImage.set(new NativeImage(NativeImage.Format.RGBA, xBuffer.get(0), yBuffer.get(0), true)));

                if (byteBuffer == null) {
                    throw new IOException("Could not load image: " + STBImage.stbi_failure_reason());
                }

                var nativeImageBuffer = MemoryUtil.memByteBuffer(nativeImage.get().getPointer(),
                        nativeImage.get().getHeight() * nativeImage.get().getWidth() * nativeImage.get().format().components());

                MemoryUtil.memCopy(byteBuffer, nativeImageBuffer);
                Minecraft.getInstance().executeBlocking(() -> {
                    DynamicTexture texture = new DynamicTexture(identifier::toString, nativeImage.get());
                    Minecraft.getInstance().getTextureManager().register(identifier, texture);
                });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static CompletableFuture<Void> saveBufferedImageAsIdentifierAsync(BufferedImage bufferedImage, Identifier identifier) {
        // https://discord.com/channels/507304429255393322/807617488313516032/934395931380576287
        return CompletableFuture.supplyAsync(() -> {
            saveBufferedImageAsIdentifier(bufferedImage, identifier);
            return null;
        }, singleThreadExecutor);
    }

    public static void removeImage(Identifier identifier) {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().getTextureManager().release(identifier));
    }

    public static boolean hasImage(Identifier identifier) {
        return getTexture(identifier) != null;
    }

    public static AbstractTexture getTexture(Identifier identifier) {
        return ((TextureManagerAccessor)SignedPaintingsClient.client.getTextureManager()).getByPath().get(identifier);
    }

    private CompletableFuture<BufferedImage> downloadImageBuffer(String urlStr) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (isValid(urlStr)) {
                    URLConnection connection = URI.create(urlStr).toURL().openConnection();
                    connection.setRequestProperty("User-Agent", "Signed Paintings mod");
                    if (urlStr.startsWith("https://i.imgur.com")) {
                        connection.setRequestProperty("Sec-Fetch-Site", "same-site");
                        connection.setRequestProperty("Referer", "https://imgur.com/");
                    }
                    connection.connect();
                    return ImageIO.read(connection.getInputStream());
                } else {
                    SignedPaintingsClient.info("invalid url string "+urlStr, false);
                    return null;
                }
            } catch (Throwable e) {
                SignedPaintingsClient.info("error downloading image "+urlStr+" : "+e, true);
                return null;
            }
        }, virtualThreadExecutor);
    }

    public static boolean isValid(@NotNull String url) {
        try {
            //noinspection ResultOfMethodCallIgnored (throws for malformed urls)
            URI.create(url).toURL();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void registerURLAlias(URLAlias urlAlias) {
        // replace existing
        for (URLAlias other : urlAliases) {
            if (other.domain.equals(urlAlias.domain)) {
                other.aliases = urlAlias.aliases;
                other.defaultImageFormat = urlAlias.defaultImageFormat;
                return;
            }
        }

        urlAliases.add(urlAlias);
    }

    public boolean trustDomain(String domain) {
        if (trustedDomains.add(domain)) {
            SignedPaintingsClient.info("trusting domain "+domain, false);
            reloadDomain(domain);
            makeChange();
            blockPromptedDomains.remove(domain);
            return true;
        }
        return false;
    }

    public boolean untrustDomain(String domain) {
        if (trustedDomains.remove(domain)) {
            SignedPaintingsClient.info("untrusting domain "+domain, false);
            reloadDomain(domain);
            makeChange();
            return true;
        }
        return false;
    }

    public boolean domainBlocked(String url) {
        for (String trusted : trustedDomains) {
            if (url.startsWith(trusted)) {
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
        blockPromptedDomains.clear();
        renderTime = 0;
        return i;
    }

    public int reloadDomain(String domain) {
        if (domain.equals("https://")) {
            return reloadAll();
        }

        int i = 0;
        for (Iterator<Map.Entry<String, ImageData>> iterator = urlToImageData.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, ImageData> imageData = iterator.next();
            if (imageData.getKey().startsWith(domain)) {
                i += imageData.getValue().reload();
                iterator.remove();
            }
        }

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
        urlToImageData.forEach((url, imageData) -> {
            if (imageData.ready) {
                imageStatuses.add(imageData.getStatus().setUrl(url));
            }
        });
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

    public void onTick() {
        renderTime++;
        // check every ~50 seconds
        if ((renderTime & 1023) == 0) {
            save();

            // expire from vram after ~2 minutes
            int expireVram = renderTime - 1536;
            // expire fully after ~15 minutes
            int expireFully = renderTime - 16384;
            urlToImageData.values().removeIf(imageData -> imageData.checkRenderTime(expireVram, expireFully));

            if (urlToImageData.isEmpty()) {
                renderTime = 0;
            }
        }
    }
}
