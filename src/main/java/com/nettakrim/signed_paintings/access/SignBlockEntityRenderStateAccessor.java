package com.nettakrim.signed_paintings.access;

import com.nettakrim.signed_paintings.rendering.PaintingInfo;

public interface SignBlockEntityRenderStateAccessor {
    void signedPaintings$setFrontInfo(PaintingInfo info);
    void signedPaintings$setBackInfo(PaintingInfo info);

    PaintingInfo signedPaintings$getFrontInfo();
    PaintingInfo signedPaintings$getBackInfo();
}
