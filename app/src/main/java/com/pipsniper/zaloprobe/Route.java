package com.pipsniper.zaloprobe;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public final class Route {
    public String id;
    public String label;
    public int notificationId;
    public String keyHash;
    public String telegramChat;
    public boolean enabled;
    public long createdAt;

    public Route() {
        id = UUID.randomUUID().toString();
        label = "Chuyên gia mới";
        keyHash = "";
        telegramChat = "";
        enabled = true;
        createdAt = System.currentTimeMillis();
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("label", label);
        o.put("notification_id", notificationId);
        o.put("key_hash", keyHash == null ? "" : keyHash);
        o.put("telegram_chat", telegramChat == null ? "" : telegramChat);
        o.put("enabled", enabled);
        o.put("created_at", createdAt);
        return o;
    }

    public static Route fromJson(JSONObject o) {
        Route r = new Route();
        r.id = o.optString("id", r.id);
        r.label = o.optString("label", "Chuyên gia");
        r.notificationId = o.optInt("notification_id", 0);
        r.keyHash = o.optString("key_hash", "");
        r.telegramChat = o.optString("telegram_chat", "");
        r.enabled = o.optBoolean("enabled", true);
        r.createdAt = o.optLong("created_at", System.currentTimeMillis());
        return r;
    }

    public boolean ready() {
        return enabled && telegramChat != null && !telegramChat.trim().isEmpty();
    }
}
