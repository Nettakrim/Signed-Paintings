package com.nettakrim;

import net.minecraft.client.MinecraftClient;
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
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ImageManager {
    private final HashMap<String, ImageData> urlToImageData;

    public ImageManager() {
        urlToImageData = new HashMap<>();
    }

    public ImageData getImageData(String url) {
        return urlToImageData.get(url);
    }

    //https://github.com/Patbox/Image2Map/blob/1.20/src/main/java/space/essem/image2map/Image2Map.java
    public void registerImage(String url) {
        if (urlToImageData.containsKey(url)) return;
        ImageData data = new ImageData();
        urlToImageData.put(url, data);
        SignedPaintingsClient.LOGGER.info("Started loading image from "+url);
        downloadImageBuffer(url).orTimeout(60, TimeUnit.SECONDS).handleAsync((image, ex) -> {
            if (image == null || ex != null) {
                urlToImageData.remove(url);
                SignedPaintingsClient.LOGGER.info("Couldnt load image "+url);
            } else {
                SignedPaintingsClient.LOGGER.info("Loaded image "+url);
                onImageLoad(image, url, data);
            }
            return null;
        });
    }

    private void onImageLoad(BufferedImage image, String url, ImageData data) {
        Identifier identifier = new Identifier(SignedPaintingsClient.MODID, createIdentifierSafeStringFromURL(url));
        saveBufferedImageAsIdentifier(image, identifier);
        data.onImageReady(identifier);
        SignedPaintingsClient.LOGGER.info("Now ready to render image "+url);
    }

    private String createIdentifierSafeStringFromURL(String url) {
        StringBuilder builder = new StringBuilder();
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

    private static boolean isValid(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
