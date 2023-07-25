package com.nettakrim.signed_paintings.access;

public interface AbstractSignEditScreenAccessor {
    int signedPaintings$paste(String s, int selectionStart, int selectionEnd);

    void signedPaintings$clear();

    void signedPaintings$setVisibility(boolean to);

    void signedPaintings$initSliders(float width, float height);
}
