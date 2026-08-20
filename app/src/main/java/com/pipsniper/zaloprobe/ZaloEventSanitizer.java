package com.pipsniper.zaloprobe;

import android.app.Notification;
import android.app.Person;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

final class ZaloEventSanitizer {
    private ZaloEventSanitizer() {}

    static JSONObject from(Context c, String eventType, StatusBarNotification sbn, Integer removalReason) {
        JSONObject root = new JSONObject();
        try {
            Notification n = sbn.getNotification();
            root.put("event_type", eventType);
            root.put("captured_at_ms", System.currentTimeMillis());
            root.put("post_time_ms", sbn.getPostTime());
            root.put("package", sbn.getPackageName());
            root.put("id", sbn.getId());
            if (removalReason != null) root.put("removal_reason", removalReason);

            root.put("sbn_tag", fp(c, sbn.getTag()));
            root.put("sbn_key", fp(c, sbn.getKey()));
            root.put("sbn_group_key", fp(c, sbn.getGroupKey()));

            JSONObject meta = new JSONObject();
            meta.put("when_ms", n.when);
            meta.put("flags", n.flags);
            meta.put("defaults", n.defaults);
            meta.put("priority", n.priority);
            meta.put("visibility", n.visibility);
            meta.put("number", n.number);
            meta.put("category", n.category == null ? JSONObject.NULL : n.category);
            meta.put("is_group_summary", (n.flags & Notification.FLAG_GROUP_SUMMARY) != 0);
            meta.put("group", fp(c, n.getGroup()));
            meta.put("sort_key", fp(c, n.getSortKey()));
            if (Build.VERSION.SDK_INT >= 26) {
                meta.put("channel_id", fp(c, n.getChannelId()));
                meta.put("group_alert_behavior", n.getGroupAlertBehavior());
                meta.put("timeout_after", n.getTimeoutAfter());
                meta.put("shortcut_id", fp(c, n.getShortcutId()));
            }
            if (Build.VERSION.SDK_INT >= 29) {
                meta.put("allow_system_generated_contextual_actions", n.getAllowSystemGeneratedContextualActions());
                if (n.getLocusId() != null) meta.put("locus_id", fp(c, n.getLocusId().getId()));
            }
            root.put("notification", meta);

            root.put("standard_fields", standardFields(c, n.extras));
            root.put("extras", sanitizeBundle(c, n.extras, 0));
            root.put("messaging_style", messagingStyle(c, n));
        } catch (Throwable e) {
            try { root.put("sanitize_error", e.getClass().getSimpleName()); } catch (Exception ignored) {}
        }
        return root;
    }

    private static JSONObject standardFields(Context c, Bundle b) {
        JSONObject o = new JSONObject();
        if (b == null) return o;
        putSafe(c, o, "title", b.get(Notification.EXTRA_TITLE));
        putSafe(c, o, "text", b.get(Notification.EXTRA_TEXT));
        putSafe(c, o, "sub_text", b.get(Notification.EXTRA_SUB_TEXT));
        putSafe(c, o, "info_text", b.get(Notification.EXTRA_INFO_TEXT));
        putSafe(c, o, "summary_text", b.get(Notification.EXTRA_SUMMARY_TEXT));
        if (Build.VERSION.SDK_INT >= 24) {
            putSafe(c, o, "conversation_title", b.get(Notification.EXTRA_CONVERSATION_TITLE));
            putSafe(c, o, "self_display_name", b.get(Notification.EXTRA_SELF_DISPLAY_NAME));
        }
        return o;
    }

    private static JSONObject messagingStyle(Context c, Notification n) {
        JSONObject out = new JSONObject();
        if (Build.VERSION.SDK_INT < 24 || n.extras == null) return out;
        try {
            Bundle b = n.extras;
            putSafe(c, out, "conversation_title", b.get(Notification.EXTRA_CONVERSATION_TITLE));
            putSafe(c, out, "self_display_name", b.get(Notification.EXTRA_SELF_DISPLAY_NAME));

            // MessagingStyle payloads are stored in notification extras. Reading the raw
            // extras is both API-stable and more useful for a probe than reconstructing
            // a framework MessagingStyle object. Values are sanitized/hashed below.
            if (b.containsKey("android.isGroupConversation")) {
                out.put("is_group_conversation", b.getBoolean("android.isGroupConversation"));
            }
            Object messages = b.get(Notification.EXTRA_MESSAGES);
            if (messages != null) out.put("messages", sanitizeValue(c, messages, 0));
            Object historic = b.get("android.messages.historic");
            if (historic != null) out.put("historic_messages", sanitizeValue(c, historic, 0));
        } catch (Throwable t) {
            try { out.put("extract_error", t.getClass().getSimpleName()); } catch (Exception ignored) {}
        }
        return out;
    }

    private static JSONObject sanitizeBundle(Context c, Bundle b, int depth) {
        JSONObject out = new JSONObject();
        if (b == null) return out;
        if (depth > 2) return out;
        try {
            Set<String> keysSet = b.keySet();
            List<String> keys = new ArrayList<>(keysSet);
            Collections.sort(keys);
            for (String key : keys) {
                JSONObject item = new JSONObject();
                Object v;
                try { v = b.get(key); } catch (Throwable t) { v = null; }
                item.put("value", sanitizeValue(c, v, depth + 1));
                out.put(key, item);
            }
        } catch (Throwable t) {
            try { out.put("__bundle_error__", t.getClass().getSimpleName()); } catch (Exception ignored) {}
        }
        return out;
    }

    private static Object sanitizeValue(Context c, Object v, int depth) {
        if (v == null) return JSONObject.NULL;
        try {
            JSONObject o = new JSONObject();
            o.put("type", v.getClass().getName());
            if (v instanceof CharSequence || v instanceof String || v instanceof Number) {
                String s = String.valueOf(v);
                o.put("length", s.length());
                o.put("hash", ProbeStore.stableHash(c, s));
                return o;
            }
            if (v instanceof Boolean) {
                o.put("value", v);
                return o;
            }
            if (v instanceof Bundle) {
                o.put("bundle", sanitizeBundle(c, (Bundle) v, depth));
                return o;
            }
            if (Build.VERSION.SDK_INT >= 28 && v instanceof Person) {
                o.put("person", sanitizePerson(c, (Person) v));
                return o;
            }
            if (v instanceof Bitmap) {
                Bitmap bm = (Bitmap) v;
                o.put("width", bm.getWidth());
                o.put("height", bm.getHeight());
                return o;
            }
            if (v instanceof Icon) {
                o.put("icon_type", ((Icon) v).getType());
                return o;
            }
            Class<?> cls = v.getClass();
            if (cls.isArray()) {
                int n = Array.getLength(v);
                o.put("array_length", n);
                JSONArray a = new JSONArray();
                int limit = Math.min(n, 30);
                for (int i = 0; i < limit; i++) a.put(sanitizeValue(c, Array.get(v, i), depth + 1));
                o.put("items", a);
                return o;
            }
            if (v instanceof Iterable) {
                JSONArray a = new JSONArray();
                int i = 0;
                for (Object item : (Iterable<?>) v) {
                    if (i++ >= 30) break;
                    a.put(sanitizeValue(c, item, depth + 1));
                }
                o.put("items", a);
                return o;
            }
            // Unknown Parcelable/objects: class name only. Avoid toString() because it can leak payloads.
            return o;
        } catch (Throwable t) {
            JSONObject e = new JSONObject();
            try { e.put("sanitize_error", t.getClass().getSimpleName()); } catch (Exception ignored) {}
            return e;
        }
    }

    private static JSONObject sanitizePerson(Context c, Person p) {
        JSONObject o = new JSONObject();
        try {
            putSafe(c, o, "name", p.getName());
            putSafe(c, o, "key", p.getKey());
            putSafe(c, o, "uri", p.getUri());
            o.put("is_bot", p.isBot());
            o.put("is_important", p.isImportant());
        } catch (Throwable ignored) {}
        return o;
    }

    private static JSONObject fp(Context c, String raw) {
        JSONObject o = new JSONObject();
        try {
            if (raw == null) {
                o.put("present", false);
            } else {
                o.put("present", true);
                o.put("length", raw.length());
                o.put("hash", ProbeStore.stableHash(c, raw));
            }
        } catch (Exception ignored) {}
        return o;
    }

    private static void putSafe(Context c, JSONObject target, String key, Object raw) {
        try {
            if (raw == null) {
                JSONObject o = new JSONObject();
                o.put("present", false);
                target.put(key, o);
                return;
            }
            String s = String.valueOf(raw);
            JSONObject o = new JSONObject();
            o.put("present", true);
            o.put("length", s.length());
            o.put("hash", ProbeStore.stableHash(c, s));
            target.put(key, o);
        } catch (Exception ignored) {}
    }
}
