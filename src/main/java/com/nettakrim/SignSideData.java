package com.nettakrim;

import net.minecraft.block.entity.SignText;

public class SignSideData {
    public SignText text;
    public PaintingInfo paintingInfo;

    public SignSideData(SignText text, PaintingInfo paintingInfo) {
        this.text = text;
        this.paintingInfo = paintingInfo;
    }
}
