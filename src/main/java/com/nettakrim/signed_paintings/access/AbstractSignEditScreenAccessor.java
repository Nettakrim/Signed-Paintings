package com.nettakrim.signed_paintings.access;

import com.nettakrim.signed_paintings.rendering.SignSideInfo;

public interface AbstractSignEditScreenAccessor {
    int signedPaintings$paste(String s, int selectionStart, int selectionEnd, boolean setText);

    void signedPaintings$clear(boolean setText);

    void signedPaintings$setVisibility(boolean to);

    void signedPaintings$initSliders(SignSideInfo info);

    String signedPaintings$getText();
}
