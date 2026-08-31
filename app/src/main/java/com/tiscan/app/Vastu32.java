package com.tiscan.app;

import java.util.Locale;

/* JADX INFO: loaded from: classes3.dex */
public final class Vastu32 {
    private static final String[] CODES = {"E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8", "S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "W1", "W2", "W3", "W4", "W5", "W6", "W7", "W8", "N1", "N2", "N3", "N4", "N5", "N6", "N7", "N8"};
    private static final String[] NAMES = {"Shikhi", "Parjanya", "Jayanta", "Indra", "Surya", "Satya", "Bhrisha", "Akasha", "Anil", "Poosha", "Vitatha", "Grihakshata", "Yama", "Gandharva", "Bhringraj", "Mriga", "Pitra", "Dwarika", "Sugriva", "Pushpadanta", "Varuna", "Asura", "Shosha", "Papyakshma", "Roga", "Naga", "Mukhya", "Bhallat", "Soma", "Bhujag", "Aditi", "Diti"};

    private Vastu32() {
    }

    public static int index(float degrees) {
        float d = VastuDirection.normalize(degrees);
        return ((int) Math.floor(VastuDirection.normalize(d - 45.0f) / 11.25f)) % 32;
    }

    public static String code(float degrees) {
        return CODES[index(degrees)];
    }

    public static String codeAtIndex(int i) {
        return CODES[((i % 32) + 32) % 32];
    }

    public static String nameAtIndex(int i) {
        return NAMES[((i % 32) + 32) % 32];
    }

    public static float centerDegreesAtIndex(int i) {
        return VastuDirection.normalize(((((i % 32) + 32) % 32) * 11.25f) + 45.0f + 5.625f);
    }

    public static String name(float degrees) {
        return NAMES[index(degrees)];
    }

    public static String label(float degrees) {
        int i = index(degrees);
        return CODES[i] + " • " + NAMES[i];
    }

    public static String description(float degrees) {
        int i = index(degrees);
        float start = VastuDirection.normalize((i * 11.25f) + 45.0f);
        float end = VastuDirection.normalize(11.25f + start);
        return String.format(Locale.US, "%s • %s • %.2f°–%.2f°", CODES[i], NAMES[i], Float.valueOf(start), Float.valueOf(end));
    }
}
