package com.nettakrim.signed_paintings.access;

public interface AbstractSignEditScreenAccessor {
    int signedPaintings$paste(String s, int selectionStart, int selectionEnd, boolean setText);

    void signedPaintings$clear(boolean setText);

    void signedPaintings$setVisibility(boolean to);

    void signedPaintings$initSliders(float width, float height);

    String signedPaintings$getText();
}
