package com.nettakrim.signed_paintings.access;

import com.nettakrim.signed_paintings.rendering.*;

public interface SignBlockEntityAccessor {
    PaintingInfo signedPaintings$getFrontPaintingInfo();

    PaintingInfo signedPaintings$getBackPaintingInfo();

    void signedPaintings$updatePaintingCentering(boolean front, Centering.Type xCentering, Centering.Type yCentering);

    void signedPaintings$updatePaintingSize(boolean front, float xSize, float ySize);

    void signedPaintings$updatePaintingYOffset(boolean front, float yOffset);

    void signedPaintings$updatePaintingPixelsPerBlock(boolean front, float pixelsPerBlock);

    BackType.Type signedPaintings$cyclePaintingBack(boolean front);

    boolean signedPaintings$hasSignSideInfo(SignSideInfo info);
}
