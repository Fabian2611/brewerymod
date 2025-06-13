package io.fabianbuthere.brewery.util;

public final class UtilMath {
    public static int lerpColor(int color, int goalColor, double progress) {
        int r = (int) ((color >> 16 & 0xFF) * (1 - progress) + (goalColor >> 16 & 0xFF) * progress);
        int g = (int) ((color >> 8 & 0xFF) * (1 - progress) + (goalColor >> 8 & 0xFF) * progress);
        int b = (int) ((color & 0xFF) * (1 - progress) + (goalColor & 0xFF) * progress);
        return (r << 16) | (g << 8) | b;
    }
}
