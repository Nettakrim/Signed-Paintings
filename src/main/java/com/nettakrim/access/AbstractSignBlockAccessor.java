package com.nettakrim.access;

import com.nettakrim.PaintingInfo;
import net.minecraft.util.Identifier;

public interface AbstractSignBlockAccessor {
    PaintingInfo getPaintingInfo();

    void setPaintingInfo(PaintingInfo info);

    Identifier createBackIdentifier();
}
