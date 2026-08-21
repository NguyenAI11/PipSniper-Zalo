package com.pipsniper.zaloprobe;

import android.content.Context;
import android.service.notification.StatusBarNotification;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RecentSourceStore {
    private static final String KEY = "recent_sources_v1";
    private static final int MAX = 40;

    public static final class Source {
        public int notificationId;
        public String keyHash = "";
        public String title = "";
        public String preview = "";
        public long lastSeen;
        public int hits;

        JSONObject toJson() throws Exception {
            JSONObject o = new JSONObject();
            o.put("notification_id", notificationId);
            o.put("key_hash", keyHash);
            o.put("title", title);
            o.put("preview", preview);
            o.put("last_seen", lastSeen);
            o.put("hits", hits);
            return o;
        }

        static Source fromJson(JSONObject o) {
            Source s = new Source();
            s.notificationId = o.optInt("notification_id");
            s.keyHash = o.optString("key_hash", "");
            s.title = o.optString("title", "");
            s.preview = o.optString("preview", "");
            s.lastSeen = o.optLong("last_seen");
            s.hits = o.optInt("hits", 0);
            return s;
        }
    }

    private RecentSourceStore() {}

    public static synchronized void touch(Context c, StatusBarNotification sbn, NotificationParser.Payload payload) {
        if (sbn == null) return;
        List<Source> list = load(c);
        Source target = null;
        for (Source s : list) if (s.notificationId == sbn.getId()) { target = s; break; }
        if (target == null) {
            target = new Source();
            target.notificationId = sbn.getId();
            list.add(target);
        }
        target.keyHash = Hashing.shortHash(sbn.getKey());
        target.title = safe(payload == null ? "" : payload.title, 80);
        String preview = payload == null || payload.lines.isEmpty() ? "" : payload.lines.get(payload.lines.size() - 1);
        target.preview = safe(preview, 140);
        target.lastSeen = System.currentTimeMillis();
        target.hits++;
        list.sort((a, b) -> Long.compare(b.lastSeen, a.lastSeen));
        if (list.size() > MAX) list = new ArrayList<>(list.subList(0, MAX));
        save(c, list);
    }

    public static synchronized List<Source> load(Context c) {
        List<Source> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(BridgePrefs.prefs(c).getString(KEY, "[]"));
            for (int i = 0; i < a.length(); i++) out.add(Source.fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) {}
        out.sort(Comparator.comparingLong((Source s) -> s.lastSeen).reversed());
        return out;
    }

    private static void save(Context c, List<Source> list) {
        JSONArray a = new JSONArray();
        for (Source s : list) try { a.put(s.toJson()); } catch (Exception ignored) {}
        BridgePrefs.prefs(c).edit().putString(KEY, a.toString()).apply();
    }

    private static String safe(String v, int max) {
        String x = Hashing.normalizeText(v);
        return x.length() <= max ? x : x.substring(0, max) + "…";
    }
}
