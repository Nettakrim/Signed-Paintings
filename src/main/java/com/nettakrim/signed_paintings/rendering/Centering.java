package com.nettakrim.signed_paintings.rendering;

public class Centering {
    public enum Type {
        MIN(0),
        CENTER(1),
        MAX(2);

        private final int index;

        Type(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }

    public static float getOffset(float size, Type centering) {
        return switch (centering) {
            case MIN -> (size-1)/2;
            case CENTER -> 0;
            case MAX -> (1-size)/2;
        };
    }

    public static String getName(boolean xAxis, Type centering) {
        return switch (centering) {
            case MIN -> xAxis ? "R" : "T";
            case CENTER -> "C";
            case MAX -> xAxis ? "L": "B";
        };
    }

    public static Type parseCentering(String name) {
        if (name.equals("C")) return Type.CENTER;
        if (name.equals("R") || name.equals("T")) return Type.MIN;
        if (name.equals("L") || name.equals("B")) return Type.MAX;
        return Type.CENTER;
    }
}
