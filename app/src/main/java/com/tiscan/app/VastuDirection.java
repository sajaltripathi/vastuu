package com.tiscan.app;

import androidx.exifinterface.media.ExifInterface;
import java.util.Locale;

/* JADX INFO: loaded from: classes3.dex */
public final class VastuDirection {
    private static final String[] S = {"N", "NNE", "NE", "ENE", ExifInterface.LONGITUDE_EAST, "ESE", "SE", "SSE", ExifInterface.LATITUDE_SOUTH, "SSW", "SW", "WSW", ExifInterface.LONGITUDE_WEST, "WNW", "NW", "NNW"};

    private VastuDirection() {
    }

    public static float normalize(float d) {
        float n = d % 360.0f;
        return n < 0.0f ? 360.0f + n : n;
    }

    public static String sector(float d) {
        return S[Math.round(normalize(d) / 22.5f) % 16];
    }

    public static String formatted(float d) {
        float n = normalize(d);
        return String.format(Locale.US, "%.1f° %s", Float.valueOf(n), sector(n));
    }
}
