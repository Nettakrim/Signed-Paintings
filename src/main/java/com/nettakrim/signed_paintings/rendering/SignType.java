package com.nettakrim.signed_paintings.rendering;

import net.minecraft.block.*;

public class SignType {
    public enum Type {
        STANDING,
        WALL,
        HANGING,
        WALL_HANGING
    }

    public static SignType.Type getType(Block block) {
        if (block instanceof SignBlock) return Type.STANDING;
        if (block instanceof WallSignBlock) return Type.WALL;
        if (block instanceof HangingSignBlock) return Type.HANGING;
        if (block instanceof WallHangingSignBlock) return Type.WALL_HANGING;
        return Type.STANDING;
    }
}
