package com.nettakrim.signed_paintings.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UploadManager {
    private final HashMap<String, String> imgurCache;

    public UploadManager() {
        this.imgurCache = new HashMap<>();
    }

    public void uploadUrlToImgur(String url2, Consumer<String> onLoadCallback) {
        String url = discordSillyness(url2);

        if (SignedPaintingsClient.imageManager.getShortestURLInference(url).startsWith("imgur:")) {
            SignedPaintingsClient.info(url+" is already an imgur link", false);
            if (onLoadCallback != null) onLoadCallback.accept(url);
            return;
        }

        if (imgurCache.containsKey(url)) {
            SignedPaintingsClient.info(url + " already in cache", false);
            if (onLoadCallback != null) onLoadCallback.accept(imgurCache.get(url));
            return;
        }

        upload(() -> uploadUrl(url)).orTimeout(60, TimeUnit.SECONDS).handleAsync((link, ex) -> {
            if (link == null || ex != null) {
                SignedPaintingsClient.info("Failed to upload " + url, true);
                if (ex != null) SignedPaintingsClient.info(ex.toString(), true);
            } else {
                SignedPaintingsClient.info("Uploaded " + url + " to " + link, false);
                imgurCache.put(url, link);
            }
            if (onLoadCallback != null) {
                try {
                    onLoadCallback.accept(link);
                } catch (Exception e) {
                    SignedPaintingsClient.info("Error in onLoadCallback:\n"+e, true);
                }
            }
            return null;
        });
    }

    public void uploadFileToImgur(File file, Consumer<String> onLoadCallback) {
        if (imgurCache.containsKey(file.getPath())) {
            if (onLoadCallback != null) onLoadCallback.accept(imgurCache.get(file.getPath()));
            return;
        }
        upload(() -> uploadFile(file)).orTimeout(60, TimeUnit.SECONDS).handleAsync((link, ex) -> {
            if (link == null || ex != null) {
                SignedPaintingsClient.info("Failed to upload "+file.toPath(), true);
                if (ex != null) SignedPaintingsClient.info(ex.toString(), true);
            } else {
                SignedPaintingsClient.info("Uploaded "+file.toPath()+" to " + link, false);
                imgurCache.put(file.getPath(), link);
            }
            if (onLoadCallback != null) {
                try {
                    onLoadCallback.accept(link);
                } catch (Exception e) {
                    SignedPaintingsClient.info("Error in onLoadCallback:\n"+e, true);
                }
            }
            return null;
        });
    }

    private HttpEntity uploadUrl(String url) {
        try {
            List<NameValuePair> params = new ArrayList<>(1);
            params.add(new BasicNameValuePair("image", url));
            return new UrlEncodedFormEntity(params, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    private HttpEntity uploadFile(File file) {
        if (file.length() > 10000000L) {
            SignedPaintingsClient.info("uploaded file is too large to upload to imgur: "+file.length(), true);
            return null;
        }
        return EntityBuilder.create().setFile(file).build();
    }

    private CompletableFuture<String> upload(Supplier<HttpEntity> createEntity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpEntity httpEntity = createEntity.get();
                if (httpEntity == null) {
                    return null;
                }

                HttpClient httpclient = HttpClients.createDefault();
                HttpPost httppost = new HttpPost("https://api.imgur.com/3/image");
                httppost.setEntity(httpEntity);

                for (String key : SignedPaintingsClient.imageManager.imgurApiKeys) {
                    httppost.setHeader("Authorization", "Client-ID " + key);

                    HttpResponse response = httpclient.execute(httppost);
                    HttpEntity entity = response.getEntity();

                    if (response.getStatusLine().getStatusCode() == 429) {
                        SignedPaintingsClient.info("client id "+key+" has had too many requests", true);
                        continue;
                    }

                    if (entity != null) {
                        return getLinkFromImgurResponse(entity.getContent());
                    } else {
                        return null;
                    }
                }
                return null;
            } catch (IOException e) {
                return null;
            }
        });
    }

    public String getLinkFromImgurResponse(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int length; (length = inputStream.read(buffer)) != -1; ) {
            result.write(buffer, 0, length);
        }
        String text = result.toString(StandardCharsets.UTF_8);
        text = text.replace("\\","");

        JsonObject jsonResponse = JsonParser.parseString(text).getAsJsonObject();
        if (jsonResponse.has("data")) {
            JsonObject data = jsonResponse.getAsJsonObject("data");
            if (data.has("link")) {
                return data.get("link").getAsString();
            }
        }

        SignedPaintingsClient.info("Failed to read json: "+text, true);
        return null;
    }

    private String discordSillyness(String url) {
        if (!url.startsWith("https://media.discordapp.net/attachments/")) return url;
        return url.replace("&format=webp","");
    }
}
