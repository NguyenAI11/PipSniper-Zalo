package com.pipsniper.zaloprobe;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public final class Hashing {
    private Hashing() {}

    public static String sha256(String value) {
        if (value == null) value = "";
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256(byte[] value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value == null ? new byte[0] : value);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format(Locale.US, "%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static String shortHash(String value) {
        return shortHash(value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8));
    }

    public static String shortHash(byte[] value) {
        String h = sha256(value);
        return h.substring(0, Math.min(24, h.length()));
    }

    public static String normalizeText(String value) {
        if (value == null) return "";
        return value
                .replace('\u00a0', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t ]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
