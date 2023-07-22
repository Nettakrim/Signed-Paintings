package com.nettakrim.access;

import com.nettakrim.Cuboid;
import com.nettakrim.PaintingInfo;
import net.minecraft.util.Identifier;

public interface SignBlockEntityAccessor {
    PaintingInfo signedPaintings$getFrontPaintingInfo();

    PaintingInfo signedPaintings$getBackPaintingInfo();

    Identifier signedPaintings$createBackIdentifier();

    void signedPaintings$updatePaintingCentering(boolean front, Cuboid.Centering xCentering, Cuboid.Centering yCentering);

    void signedPaintings$updatePaintingSize(boolean front, float xSize, float ySize);
}
