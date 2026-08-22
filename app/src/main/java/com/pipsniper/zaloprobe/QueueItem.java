package com.pipsniper.zaloprobe;

import org.json.JSONObject;

import java.util.UUID;

public final class QueueItem {
    public String id = UUID.randomUUID().toString();
    public String routeId = "";
    public String chatId = "";
    public String text = "";
    public String mediaPath = "";
    public String contentHash = "";
    public long createdAt = System.currentTimeMillis();
    public int attempts = 0;
    public long nextAttemptAt = 0L;

    public JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("route_id", routeId);
        o.put("chat_id", chatId);
        o.put("text", text);
        o.put("media_path", mediaPath);
        o.put("content_hash", contentHash);
        o.put("created_at", createdAt);
        o.put("attempts", attempts);
        o.put("next_attempt_at", nextAttemptAt);
        return o;
    }

    public static QueueItem fromJson(JSONObject o) {
        QueueItem i = new QueueItem();
        i.id = o.optString("id", i.id);
        i.routeId = o.optString("route_id", "");
        i.chatId = o.optString("chat_id", "");
        i.text = o.optString("text", "");
        i.mediaPath = o.optString("media_path", "");
        i.contentHash = o.optString("content_hash", "");
        i.createdAt = o.optLong("created_at", System.currentTimeMillis());
        i.attempts = o.optInt("attempts", 0);
        i.nextAttemptAt = o.optLong("next_attempt_at", 0L);
        return i;
    }

    public boolean hasMedia() {
        return mediaPath != null && !mediaPath.isEmpty();
    }
}
