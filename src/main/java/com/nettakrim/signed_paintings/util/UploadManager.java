package com.nettakrim.signed_paintings.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nettakrim.signed_paintings.SignedPaintingsClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
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

public class UploadManager {
    private final String clientId;
    private final HashMap<String, String> urlToImgurCache;

    public UploadManager(String clientId) {
        this.clientId = clientId;
        this.urlToImgurCache = new HashMap<>();
    }

    public void uploadToImgur(String url, Consumer<String> onLoadCallback) {
        if (urlToImgurCache.containsKey(url)) {
            if (onLoadCallback != null) onLoadCallback.accept(urlToImgurCache.get(url));
            return;
        }
        upload(url).orTimeout(60, TimeUnit.SECONDS).handleAsync((link, ex) -> {
            if (link == null || ex != null) {
                SignedPaintingsClient.info("Failed to upload " + url+"\n"+ex.toString(), true);
            } else {
                SignedPaintingsClient.info("Uploaded " + url + " to " + link, false);
                urlToImgurCache.put(url, link);
            }
            if (onLoadCallback != null) onLoadCallback.accept(link);
            return null;
        });
    }

    private CompletableFuture<String> upload(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient httpclient = HttpClients.createDefault();
                HttpPost httppost = new HttpPost("https://api.imgur.com/3/image");

                List<NameValuePair> params = new ArrayList<>(2);
                params.add(new BasicNameValuePair("image", url));
                httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
                httppost.setHeader("Authorization", "Client-ID " + clientId);

                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    return getLinkFromImgurResponse(entity.getContent());
                } else {
                    return null;
                }
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
}
