package com.pipsniper.zaloprobe;

import android.app.Notification;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class NotificationParser {
    public static final class Payload {
        public String title = "";
        public final List<String> lines = new ArrayList<>();
        public byte[] imagePng;
    }

    private NotificationParser() {}

    public static Payload parse(StatusBarNotification sbn) {
        Payload out = new Payload();
        if (sbn == null || sbn.getNotification() == null) return out;
        Notification n = sbn.getNotification();
        Bundle e = n.extras;
        if (e == null) return out;

        out.title = firstNonEmpty(
                text(e.get(Notification.EXTRA_CONVERSATION_TITLE)),
                text(e.get(Notification.EXTRA_TITLE)),
                text(e.get("android.title")),
                text(e.get(Notification.EXTRA_SUB_TEXT))
        );

        Set<String> ordered = new LinkedHashSet<>();
        Object rawLines = e.get(Notification.EXTRA_TEXT_LINES);
        if (rawLines instanceof CharSequence[]) {
            for (CharSequence cs : (CharSequence[]) rawLines) add(ordered, cs == null ? "" : cs.toString());
        }

        if (ordered.isEmpty()) {
            add(ordered, text(e.get(Notification.EXTRA_BIG_TEXT)));
            add(ordered, text(e.get(Notification.EXTRA_TEXT)));
        }

        if (ordered.isEmpty()) {
            add(ordered, text(e.get("android.bigText")));
            add(ordered, text(e.get("android.text")));
        }

        out.lines.addAll(ordered);
        out.imagePng = extractPicture(e);
        return out;
    }

    private static byte[] extractPicture(Bundle e) {
        try {
            Object p = e.get(Notification.EXTRA_PICTURE);
            if (!(p instanceof Bitmap)) p = e.get("android.picture");
            if (!(p instanceof Bitmap)) return null;
            Bitmap b = (Bitmap) p;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            if (!b.compress(Bitmap.CompressFormat.PNG, 90, bos)) return null;
            byte[] bytes = bos.toByteArray();
            return bytes.length == 0 ? null : bytes;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void add(Set<String> out, String s) {
        String v = Hashing.normalizeText(s);
        if (!v.isEmpty()) out.add(v);
    }

    private static String text(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String v : values) {
            String x = Hashing.normalizeText(v);
            if (!x.isEmpty()) return x;
        }
        return "";
    }
}
