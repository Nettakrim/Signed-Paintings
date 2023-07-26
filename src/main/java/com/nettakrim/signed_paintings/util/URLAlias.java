package com.nettakrim.signed_paintings.util;

import com.nettakrim.signed_paintings.SignedPaintingsClient;

import java.util.Arrays;
import java.util.Comparator;

public record URLAlias (String domain, String[] aliases, String defaultImageFormat) {
    public String tryApply(String url) {
        String lowercaseUrl = url.toLowerCase();
        for (String alias : aliases) {
            if (lowercaseUrl.startsWith(alias)) {
                String id = url.substring(alias.length());
                String newText = domain + id;
                if (!id.contains(".")) newText += defaultImageFormat;
                return newText;
            }
        }
        return url;
    }

    public String getShortestAlias(String url) {
        if (!url.toLowerCase().startsWith(domain)) return url;
        String shortest = Arrays.stream(aliases).min(Comparator.comparing(String::length)).get();
        url = shortest+url.substring(domain.length());
        SignedPaintingsClient.LOGGER.info(shortest+" "+url);
        //if (url.endsWith(defaultImageFormat)) url = url.substring(0, url.length()-defaultImageFormat.length());
        return url;
    }
}
