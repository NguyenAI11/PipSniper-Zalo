package com.pipsniper.zaloprobe;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class DedupStore {
    private static final String KEY = "dedup_state_v1";
    private static final long REMOVE_GRACE_MS = 10_000L;

    public static final class Delta {
        public final List<String> newLines = new ArrayList<>();
        public boolean newImage;
    }

    private DedupStore() {}

    public static synchronized Delta diff(Context c, Route route, NotificationParser.Payload payload) {
        Delta out = new Delta();
        if (route == null || payload == null) return out;
        JSONObject root = root(c);
        JSONObject state = root.optJSONObject(route.id);
        if (state == null) state = new JSONObject();

        long now = System.currentTimeMillis();
        long removedAt = state.optLong("removed_at", 0L);
        List<String> previous = new ArrayList<>();
        if (removedAt == 0L || now - removedAt <= REMOVE_GRACE_MS) {
            JSONArray old = state.optJSONArray("line_hashes");
            if (old != null) for (int i = 0; i < old.length(); i++) previous.add(old.optString(i, ""));
        }

        List<String> current = new ArrayList<>();
        for (String line : payload.lines) current.add(Hashing.sha256(Hashing.normalizeText(line)));
        List<Integer> indexes = DeltaLogic.newIndexes(previous, current);
        for (Integer i : indexes) {
            if (i != null && i >= 0 && i < payload.lines.size()) out.newLines.add(payload.lines.get(i));
        }

        String imageHash = payload.imagePng == null ? "" : Hashing.sha256(payload.imagePng);
        String oldImageHash = state.optString("image_hash", "");
        out.newImage = !imageHash.isEmpty() && !imageHash.equals(oldImageHash);

        try {
            JSONArray a = new JSONArray();
            for (String h : current) a.put(h);
            state.put("line_hashes", a);
            state.put("image_hash", imageHash);
            state.put("last_post_at", now);
            state.put("removed_at", 0L);
            root.put(route.id, state);
            save(c, root);
        } catch (Exception ignored) {}
        return out;
    }

    public static synchronized void markRemoved(Context c, Route route) {
        if (route == null) return;
        JSONObject root = root(c);
        JSONObject state = root.optJSONObject(route.id);
        if (state == null) state = new JSONObject();
        try {
            state.put("removed_at", System.currentTimeMillis());
            root.put(route.id, state);
            save(c, root);
        } catch (Exception ignored) {}
    }

    public static synchronized void resetRoute(Context c, String routeId) {
        JSONObject root = root(c);
        root.remove(routeId);
        save(c, root);
    }

    private static JSONObject root(Context c) {
        try { return new JSONObject(BridgePrefs.prefs(c).getString(KEY, "{}")); }
        catch (Exception e) { return new JSONObject(); }
    }

    private static void save(Context c, JSONObject root) {
        BridgePrefs.prefs(c).edit().putString(KEY, root.toString()).apply();
    }
}
