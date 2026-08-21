package com.pipsniper.zaloprobe;

import android.content.Context;
import android.content.SharedPreferences;

public final class BridgePrefs {
    public static final String ZALO_PACKAGE = "com.zing.zalo";
    private static final String NAME = "pipsniper_bridge_v1";

    private BridgePrefs() {}

    public static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static boolean enabled(Context c) {
        return prefs(c).getBoolean("bridge_enabled", true);
    }

    public static void setEnabled(Context c, boolean enabled) {
        prefs(c).edit().putBoolean("bridge_enabled", enabled).apply();
    }

    public static void setListenerConnected(Context c, boolean connected) {
        prefs(c).edit().putBoolean("listener_connected", connected).putLong("listener_touch_ms", System.currentTimeMillis()).apply();
    }

    public static boolean listenerConnected(Context c) {
        return prefs(c).getBoolean("listener_connected", false);
    }

    public static void markSeen(Context c) {
        prefs(c).edit()
                .putLong("last_zalo_ms", System.currentTimeMillis())
                .putLong("zalo_seen", prefs(c).getLong("zalo_seen", 0) + 1)
                .apply();
    }

    public static void markForwarded(Context c) {
        prefs(c).edit()
                .putLong("last_forward_ms", System.currentTimeMillis())
                .putLong("forwarded", prefs(c).getLong("forwarded", 0) + 1)
                .apply();
    }

    public static void markFailed(Context c) {
        prefs(c).edit().putLong("failed", prefs(c).getLong("failed", 0) + 1).apply();
    }
}
