package com.hakye.mediahub.api.post.color;

import com.hakye.mediahub.api.common.exceptions.BadRequestException;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A color in CIELAB (D65). Euclidean distance between two of these approximates perceived difference. */
public record ColorLab(double l, double a, double b) {

    private static final Pattern HEX = Pattern.compile("^#?([0-9a-fA-F]{6})$");
    private static final double XN = 0.95047, YN = 1.0, ZN = 1.08883;

    /** "#RRGGBB" in upper case. Accepts the value with or without '#'. */
    public static String normalizeHex(String raw) {
        Matcher m = raw == null ? null : HEX.matcher(raw.trim());
        if (m == null || !m.matches()) throw new BadRequestException("Invalid color. Use a hex value like #8F3030");
        return "#" + m.group(1).toUpperCase(Locale.ROOT);
    }

    public static ColorLab fromHex(String hex) {
        String h = normalizeHex(hex);
        return fromRgb(Integer.parseInt(h.substring(1, 3), 16),
                Integer.parseInt(h.substring(3, 5), 16),
                Integer.parseInt(h.substring(5, 7), 16));
    }

    public static ColorLab fromRgb(int r, int g, int b) {
        double lr = linear(r / 255.0), lg = linear(g / 255.0), lb = linear(b / 255.0);
        double x = 0.4124564 * lr + 0.3575761 * lg + 0.1804375 * lb;
        double y = 0.2126729 * lr + 0.7151522 * lg + 0.0721750 * lb;
        double z = 0.0193339 * lr + 0.1191920 * lg + 0.9503041 * lb;
        double fx = f(x / XN), fy = f(y / YN), fz = f(z / ZN);
        return new ColorLab(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz));
    }

    public double distanceTo(ColorLab o) {
        return Math.sqrt(Math.pow(l - o.l, 2) + Math.pow(a - o.a, 2) + Math.pow(b - o.b, 2));
    }

    private static double linear(double c) {
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static double f(double t) {
        return t > 216.0 / 24389.0 ? Math.cbrt(t) : (24389.0 / 27.0 * t + 16) / 116;
    }
}
