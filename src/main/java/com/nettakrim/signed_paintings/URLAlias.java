package com.nettakrim.signed_paintings;

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
}
