package com.nettakrim.signed_paintings.access;

import com.nettakrim.signed_paintings.rendering.OverlayInfo;

public interface BannerBlockEntityRenderStateAccessor {
    void signedPaintings$setOverlayInfo(OverlayInfo info);

    OverlayInfo signedPaintings$getOverlayInfo();
}
