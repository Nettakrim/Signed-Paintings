package com.nettakrim.signed_paintings.access;

import com.nettakrim.signed_paintings.rendering.PaintingInfo;

public interface SignBlockEntityRenderStateAccessor {
    void signedPaintings$setFrontInfo(PaintingInfo info);
    void signedPaintings$setBackInfo(PaintingInfo info);
    void signedPaintings$setRotation(float rotation);

    PaintingInfo signedPaintings$getFrontInfo();
    PaintingInfo signedPaintings$getBackInfo();
    float signedPaintings$getRotation();
}
