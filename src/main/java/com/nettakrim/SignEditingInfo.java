package com.nettakrim;

import com.nettakrim.access.AbstractSignEditScreenAccessor;
import com.nettakrim.access.SignBlockEntityAccessor;
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

    public void updatePaintingCentering(boolean front, Cuboid.Centering xCentering, Cuboid.Centering yCentering) {
        ((SignBlockEntityAccessor)sign).updatePaintingCentering(front, xCentering, yCentering);
    }

    public void updatePaintingSize(boolean front, float xSize, float ySize) {
        ((SignBlockEntityAccessor)sign).updatePaintingSize(front, xSize, ySize);
    }
}
