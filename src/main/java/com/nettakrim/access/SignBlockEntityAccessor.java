package com.nettakrim.access;

import com.nettakrim.Cuboid;
import com.nettakrim.PaintingInfo;
import net.minecraft.util.Identifier;

public interface SignBlockEntityAccessor {
    PaintingInfo getFrontPaintingInfo();

    PaintingInfo getBackPaintingInfo();

    Identifier createBackIdentifier();

    void updatePaintingCentering(boolean front, Cuboid.Centering xCentering, Cuboid.Centering yCentering);

    void updatePaintingSize(boolean front, float xSize, float ySize);
}
