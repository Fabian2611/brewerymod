package io.fabianbuthere.brewery.util;

public final class UtilMath {
    public static int lerpColor(int color, int goalColor, double progress) {
        int r = (int) ((color >> 16 & 0xFF) * (1 - progress) + (goalColor >> 16 & 0xFF) * progress);
        int g = (int) ((color >> 8 & 0xFF) * (1 - progress) + (goalColor >> 8 & 0xFF) * progress);
        int b = (int) ((color & 0xFF) * (1 - progress) + (goalColor & 0xFF) * progress);
        return (r << 16) | (g << 8) | b;
    }

    public static int expInterpolateColor(int color, int goalColor, double progress) {
        if (progress <= 0) return color;
        if (progress >= 1) return goalColor;

        double expProgress = Math.pow(progress, 2);
        return lerpColor(color, goalColor, expProgress);
    }
}
