package com.nettakrim.signed_paintings.rendering;

public class BackType {
    public enum Type {
        SIGN,
        BLOCK,
        NONE
    }

    public static Type cycle(Type type) {
        return switch (type) {
            case SIGN -> Type.BLOCK;
            case BLOCK -> Type.NONE;
            case NONE -> Type.SIGN;
        };
    }

    public static String getName(Type type) {
        return switch (type) {
            case SIGN -> "S";
            case BLOCK -> "B";
            case NONE -> "N";
        };
    }

    public static Type parseBackType(String name) {
        if (name.equals("S")) return Type.SIGN;
        if (name.equals("B")) return Type.BLOCK;
        if (name.equals("N")) return Type.NONE;
        return Type.SIGN;
    }
}
