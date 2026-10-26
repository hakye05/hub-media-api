package com.hakye.mediahub.api.media.image;

import java.util.Optional;

public final class ImageHeader {

    private ImageHeader() {}

    /** Checks the file signature of the first bytes of the object. */
    public static boolean matches(ImageType type, byte[] h) {
        return switch (type) {
            case JPEG -> has(h, 0, 0xFF, 0xD8, 0xFF);
            case PNG  -> has(h, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
            case WEBP -> has(h, 0, 'R', 'I', 'F', 'F') && has(h, 8, 'W', 'E', 'B', 'P');
        };
    }

    /** How many bytes from the start of the object are enough for matches() and dimensions(). */
    public static int sniffBytes(ImageType type) {
        return type == ImageType.JPEG ? 1024 * 1024 : 64;
    }

    /** Display size from the file header. Empty if the header is malformed or truncated. */
    public static Optional<ImageSize> dimensions(ImageType type, byte[] h) {
        return switch (type) {
            case PNG -> pngSize(h);
            case WEBP -> webpSize(h);
            case JPEG -> jpegSize(h);
        };
    }

    private static boolean has(byte[] h, int offset, int... expected) {
        if (h.length < offset + expected.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if ((h[offset + i] & 0xFF) != expected[i]) return false;
        }
        return true;
    }

    private static Optional<ImageSize> pngSize(byte[] h) {
        // 8-byte signature, then the IHDR chunk: length(4) "IHDR"(4) width(4) height(4)
        if (h.length < 24 || !has(h, 12, 'I', 'H', 'D', 'R')) return Optional.empty();
        return size(u32(h, 16, false), u32(h, 20, false));
    }

    private static Optional<ImageSize> webpSize(byte[] h) {
        if (h.length < 25) return Optional.empty();
        if (has(h, 12, 'V', 'P', '8', 'L')) {                       // lossless
            long bits = u32(h, 21, true);
            return size((bits & 0x3FFF) + 1, ((bits >> 14) & 0x3FFF) + 1);
        }
        if (h.length < 30) return Optional.empty();
        if (has(h, 12, 'V', 'P', '8', ' ') && has(h, 23, 0x9D, 0x01, 0x2A)) {   // lossy
            return size(u16(h, 26, true) & 0x3FFF, u16(h, 28, true) & 0x3FFF);
        }
        if (has(h, 12, 'V', 'P', '8', 'X')) {                       // extended: canvas size
            return size(u24le(h, 24) + 1L, u24le(h, 27) + 1L);
        }
        return Optional.empty();
    }

    private static Optional<ImageSize> jpegSize(byte[] h) {
        int orientation = 1;
        int i = 2; // skip SOI (FF D8)
        while (i + 4 <= h.length) {
            if ((h[i] & 0xFF) != 0xFF) return Optional.empty();
            int marker = h[i + 1] & 0xFF;
            if (marker == 0xFF) { i++; continue; }                                            // padding
            if (marker == 0x01 || (marker >= 0xD0 && marker <= 0xD8)) { i += 2; continue; }   // no payload
            if (marker == 0xD9 || marker == 0xDA) return Optional.empty();                    // ended before a frame header
            int len = u16(h, i + 2, false);
            if (len < 2) return Optional.empty();

            if (marker == 0xE1) {
                int o = exifOrientation(h, i + 4, len - 2);
                if (o != 0) orientation = o;
            }
            boolean sof = marker >= 0xC0 && marker <= 0xCF
                    && marker != 0xC4 && marker != 0xC8 && marker != 0xCC;
            if (sof) {
                if (i + 9 > h.length) return Optional.empty();
                int height = u16(h, i + 5, false);
                int width = u16(h, i + 7, false);
                return orientation >= 5 ? size(height, width) : size(width, height); // 5-8 are rotated 90°
            }
            i += 2 + len;
        }
        return Optional.empty();
    }

    /** EXIF orientation 1-8, or 0 if this APP1 segment has none. */
    private static int exifOrientation(byte[] h, int start, int len) {
        int end = Math.min(start + len, h.length);
        if (end - start < 14 || !has(h, start, 'E', 'x', 'i', 'f', 0, 0)) return 0;
        int tiff = start + 6;
        boolean le;
        if (has(h, tiff, 'I', 'I')) le = true;
        else if (has(h, tiff, 'M', 'M')) le = false;
        else return 0;

        long off = u32(h, tiff + 4, le);
        long ifd = tiff + off;
        if (off < 8 || ifd + 2 > end) return 0;

        int count = u16(h, (int) ifd, le);
        for (int n = 0; n < count; n++) {
            int e = (int) ifd + 2 + n * 12;
            if (e + 12 > end) return 0;
            if (u16(h, e, le) == 0x0112) {
                int v = u16(h, e + 8, le);
                return v >= 1 && v <= 8 ? v : 0;
            }
        }
        return 0;
    }

    private static Optional<ImageSize> size(long width, long height) {
        if (width < 1 || height < 1 || width > Integer.MAX_VALUE || height > Integer.MAX_VALUE) {
            return Optional.empty();
        }
        return Optional.of(new ImageSize((int) width, (int) height));
    }

    private static int u16(byte[] h, int o, boolean le) {
        int a = h[o] & 0xFF, b = h[o + 1] & 0xFF;
        return le ? (b << 8) | a : (a << 8) | b;
    }

    private static long u32(byte[] h, int o, boolean le) {
        long a = u16(h, o, le), b = u16(h, o + 2, le);
        return le ? (b << 16) | a : (a << 16) | b;
    }

    private static int u24le(byte[] h, int o) {
        return (h[o] & 0xFF) | ((h[o + 1] & 0xFF) << 8) | ((h[o + 2] & 0xFF) << 16);
    }
}
