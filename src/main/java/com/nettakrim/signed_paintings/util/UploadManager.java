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
import java.util.List;

public class UploadManager {
    private final String clientId;

    public UploadManager(String clientId) {
        this.clientId = clientId;
    }

    public String uploadToImgur(String url) {
        try {
            HttpClient httpclient = HttpClients.createDefault();
            HttpPost httppost = new HttpPost("https://api.imgur.com/3/image");

            List<NameValuePair> params = new ArrayList<>(2);
            params.add(new BasicNameValuePair("image", url));
            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            httppost.setHeader("Authorization", "Client-ID "+clientId);

            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                return getLinkFromImgurResponse(entity.getContent());
            } else {
                return null;
            }
        } catch (IOException e) {
            SignedPaintingsClient.LOGGER.info("Failed to upload "+url);
            return null;
        }
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

        SignedPaintingsClient.LOGGER.info("Failed to read json: "+text);
        return null;
    }
}
