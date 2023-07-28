package com.nettakrim.signed_paintings.access;

import com.nettakrim.signed_paintings.rendering.*;

public interface SignBlockEntityAccessor {
    PaintingInfo signedPaintings$getFrontPaintingInfo();

    PaintingInfo signedPaintings$getBackPaintingInfo();

    SignSideInfo signedPaintings$getSideInfo(boolean front);

    boolean signedPaintings$hasSignSideInfo(SignSideInfo info);
}
