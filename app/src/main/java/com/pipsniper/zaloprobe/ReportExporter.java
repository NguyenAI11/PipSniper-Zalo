package com.pipsniper.zaloprobe;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

final class ReportExporter {
    private ReportExporter() {}

    static long export(Context c, Uri uri) throws Exception {
        try (OutputStream os = c.getContentResolver().openOutputStream(uri, "w");
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            JSONObject meta = new JSONObject();
            meta.put("schema", "PIPSNIPER_ZALO_NOTIFICATION_PROBE_V1");
            meta.put("probe_version", "0.2");
            meta.put("exported_at_ms", System.currentTimeMillis());
            meta.put("privacy_mode", "HASHED_LOCAL_SALT");
            meta.put("internet_permission_declared", false);
            meta.put("zalo_package", ProbeStore.ZALO_PACKAGE);
            meta.put("device_manufacturer", Build.MANUFACTURER);
            meta.put("device_model", Build.MODEL);
            meta.put("android_release", Build.VERSION.RELEASE);
            meta.put("android_sdk", Build.VERSION.SDK_INT);
            meta.put("event_count", ProbeStore.prefs(c).getLong("event_count", 0L));
            meta.put("overflow", ProbeStore.prefs(c).getBoolean("overflow", false));
            meta.put("zalo_version", getZaloVersion(c));

            out.write("{\n  \"meta\": ");
            out.write(meta.toString(2));
            out.write(",\n  \"events\": [\n");

            long written = 0;
            File f = ProbeStore.eventsFile(c);
            if (f.exists()) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                    String line;
                    boolean first = true;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        try {
                            new JSONObject(line); // validate line before copying
                            if (!first) out.write(",\n");
                            out.write("    ");
                            out.write(line);
                            first = false;
                            written++;
                        } catch (Exception ignored) {}
                    }
                }
            }
            out.write("\n  ],\n  \"events_exported\": " + written + "\n}\n");
            out.flush();
            return written;
        }
    }

    private static String getZaloVersion(Context c) {
        try {
            PackageInfo pi = c.getPackageManager().getPackageInfo(ProbeStore.ZALO_PACKAGE, 0);
            return pi.versionName == null ? "unknown" : pi.versionName;
        } catch (Exception e) {
            return "not_detected";
        }
    }
}
