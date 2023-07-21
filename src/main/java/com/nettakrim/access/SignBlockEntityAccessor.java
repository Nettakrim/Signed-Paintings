package com.nettakrim.access;

import com.nettakrim.PaintingInfo;
import net.minecraft.util.Identifier;

public interface SignBlockEntityAccessor {
    PaintingInfo getFrontPaintingInfo();

    PaintingInfo getBackPaintingInfo();

    Identifier createBackIdentifier();
}
