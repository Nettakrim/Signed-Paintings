package com.nettakrim.signed_paintings.gui;

import com.nettakrim.signed_paintings.access.AbstractSignEditScreenAccessor;
import com.nettakrim.signed_paintings.access.SignBlockEntityAccessor;
import com.nettakrim.signed_paintings.rendering.BackType;
import com.nettakrim.signed_paintings.rendering.Centering;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.util.SelectionManager;

public class SignEditingInfo {
    public final SignBlockEntity sign;

    public final AbstractSignEditScreenAccessor screen;

    public SelectionManager selectionManager;

    public SignEditingInfo(SignBlockEntity sign, AbstractSignEditScreenAccessor screen) {
        this.sign = sign;
        this.screen = screen;
    }

    public void setSelectionManager(SelectionManager selectionManager) {
        this.selectionManager = selectionManager;
    }

    public void updatePaintingCentering(boolean front, Centering.Type xCentering, Centering.Type yCentering) {
        ((SignBlockEntityAccessor)sign).signedPaintings$updatePaintingCentering(front, xCentering, yCentering);
    }

    public void updatePaintingSize(boolean front, float xSize, float ySize) {
        ((SignBlockEntityAccessor)sign).signedPaintings$updatePaintingSize(front, xSize, ySize);
    }

    public BackType.Type cyclePaintingBack(boolean front) {
        return ((SignBlockEntityAccessor)sign).signedPaintings$cyclePaintingBack(front);
    }
}
