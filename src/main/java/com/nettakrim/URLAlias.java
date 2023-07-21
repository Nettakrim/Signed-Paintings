package com.nettakrim;

public class URLAlias {
    public String domain;
    public String[] aliases;
    public String defaultImageFormat;

    public URLAlias(String domain, String[] aliases, String defaultImageFormat) {
        this.domain = domain;
        this.aliases = aliases;
        this.defaultImageFormat = defaultImageFormat;
    }

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
