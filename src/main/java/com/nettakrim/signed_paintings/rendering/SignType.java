package com.nettakrim.signed_paintings.rendering;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;

public class SignType {
    public enum Type {
        STANDING,
        WALL,
        HANGING,
        WALL_HANGING
    }

    public static SignType.Type getType(Block block) {
        if (block instanceof StandingSignBlock) return Type.STANDING;
        if (block instanceof WallSignBlock) return Type.WALL;
        if (block instanceof CeilingHangingSignBlock) return Type.HANGING;
        if (block instanceof WallHangingSignBlock) return Type.WALL_HANGING;
        return Type.STANDING;
    }
}
