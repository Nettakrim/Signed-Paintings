package com.nettakrim.signed_paintings.gui;

import com.nettakrim.signed_paintings.access.AbstractSignEditScreenAccessor;
import com.nettakrim.signed_paintings.access.SignBlockEntityAccessor;
import com.nettakrim.signed_paintings.rendering.SignSideInfo;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.world.level.block.entity.SignBlockEntity;

public class SignEditingInfo {
    public final SignBlockEntity sign;

    public final AbstractSignEditScreenAccessor screen;

    public TextFieldHelper selectionManager;

    public SignEditingInfo(SignBlockEntity sign, AbstractSignEditScreenAccessor screen) {
        this.sign = sign;
        this.screen = screen;
    }

    public void setSelectionManager(TextFieldHelper selectionManager) {
        this.selectionManager = selectionManager;
    }

    public SignSideInfo getSideInfo(boolean front) {
        return ((SignBlockEntityAccessor)sign).signedPaintings$getSideInfo(front);
    }
}
