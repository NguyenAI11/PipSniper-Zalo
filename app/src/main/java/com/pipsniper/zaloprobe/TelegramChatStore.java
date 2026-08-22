package com.pipsniper.zaloprobe;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TelegramChatStore {
    private static final String KEY = "telegram_chats_v1";
    private static final int MAX = 50;

    private TelegramChatStore() {}

    public static synchronized void merge(Context c, List<TelegramDiscovery.ChatCandidate> incoming) {
        Map<Long, TelegramDiscovery.ChatCandidate> map = new LinkedHashMap<>();
        for (TelegramDiscovery.ChatCandidate x : load(c)) map.put(x.id, x);
        if (incoming != null) for (TelegramDiscovery.ChatCandidate x : incoming) if (x != null && x.id != 0L) map.put(x.id, x);
        JSONArray a = new JSONArray();
        int n = 0;
        for (TelegramDiscovery.ChatCandidate x : map.values()) {
            if (n++ >= MAX) break;
            try {
                JSONObject o = new JSONObject();
                o.put("id", x.id);
                o.put("title", x.title == null ? "" : x.title);
                o.put("type", x.type == null ? "" : x.type);
                o.put("username", x.username == null ? "" : x.username);
                a.put(o);
            } catch (Exception ignored) {}
        }
        BridgePrefs.prefs(c).edit().putString(KEY, a.toString()).apply();
    }

    public static synchronized List<TelegramDiscovery.ChatCandidate> load(Context c) {
        List<TelegramDiscovery.ChatCandidate> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(BridgePrefs.prefs(c).getString(KEY, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null) continue;
                TelegramDiscovery.ChatCandidate x = new TelegramDiscovery.ChatCandidate();
                x.id = o.optLong("id", 0L);
                x.title = o.optString("title", "");
                x.type = o.optString("type", "");
                x.username = o.optString("username", "");
                if (x.id != 0L) out.add(x);
            }
        } catch (Exception ignored) {}
        return out;
    }
}
