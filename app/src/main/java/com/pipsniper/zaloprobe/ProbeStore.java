package com.pipsniper.zaloprobe;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

final class ProbeStore {
    static final String ZALO_PACKAGE = "com.zing.zalo";
    static final String PREFS = "probe_prefs";
    static final String EVENTS_FILE = "zalo_events.jsonl";
    static final int MAX_EVENTS = 10000;

    private ProbeStore() {}

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean isCaptureEnabled(Context c) {
        return prefs(c).getBoolean("capture_enabled", true);
    }

    static void setCaptureEnabled(Context c, boolean enabled) {
        prefs(c).edit().putBoolean("capture_enabled", enabled).apply();
    }

    static synchronized long append(Context c, JSONObject event) {
        SharedPreferences p = prefs(c);
        long count = p.getLong("event_count", 0L);
        if (count >= MAX_EVENTS) {
            p.edit().putBoolean("overflow", true).apply();
            return count;
        }
        File f = new File(c.getFilesDir(), EVENTS_FILE);
        try (FileOutputStream out = new FileOutputStream(f, true)) {
            out.write(event.toString().getBytes(StandardCharsets.UTF_8));
            out.write('\n');
            out.flush();
            count++;
            p.edit()
                    .putLong("event_count", count)
                    .putLong("last_capture_ms", System.currentTimeMillis())
                    .apply();
        } catch (Exception e) {
            p.edit().putString("last_error", e.getClass().getSimpleName()).apply();
        }
        return count;
    }

    static synchronized void clear(Context c) {
        File f = new File(c.getFilesDir(), EVENTS_FILE);
        if (f.exists()) f.delete();
        prefs(c).edit()
                .remove("event_count")
                .remove("last_capture_ms")
                .remove("last_error")
                .remove("overflow")
                .apply();
    }

    static File eventsFile(Context c) {
        return new File(c.getFilesDir(), EVENTS_FILE);
    }

    static String stableHash(Context c, String value) {
        if (value == null) return null;
        try {
            byte[] salt = getOrCreateSalt(c);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            md.update((byte) 0x00);
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 12; i++) sb.append(String.format("%02x", digest[i]));
            return sb.toString();
        } catch (Exception e) {
            return "hash_error";
        }
    }

    private static byte[] getOrCreateSalt(Context c) {
        SharedPreferences p = prefs(c);
        String existing = p.getString("install_salt", null);
        if (existing != null) return Base64.decode(existing, Base64.NO_WRAP);
        byte[] salt = new byte[32];
        new SecureRandom().nextBytes(salt);
        p.edit().putString("install_salt", Base64.encodeToString(salt, Base64.NO_WRAP)).commit();
        return salt;
    }
}
