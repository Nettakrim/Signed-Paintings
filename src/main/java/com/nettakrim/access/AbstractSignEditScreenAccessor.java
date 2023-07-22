package com.nettakrim.access;

public interface AbstractSignEditScreenAccessor {
    int signedPaintings$paste(String s, int selectionStart, int selectionEnd);

    void signedPaintings$clear();

    String signedPaintings$getCombinedMessage();
}
