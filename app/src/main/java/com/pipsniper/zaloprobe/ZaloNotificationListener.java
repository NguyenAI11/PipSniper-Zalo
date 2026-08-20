package com.pipsniper.zaloprobe;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import org.json.JSONObject;

public class ZaloNotificationListener extends NotificationListenerService {
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        ProbeStore.prefs(this).edit().putBoolean("listener_connected", true).apply();
    }

    @Override
    public void onListenerDisconnected() {
        ProbeStore.prefs(this).edit().putBoolean("listener_connected", false).apply();
        super.onListenerDisconnected();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!accept(sbn)) return;
        JSONObject event = ZaloEventSanitizer.from(this, "POSTED", sbn, null);
        ProbeStore.append(this, event);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap, int reason) {
        if (!accept(sbn)) return;
        JSONObject event = ZaloEventSanitizer.from(this, "REMOVED", sbn, reason);
        ProbeStore.append(this, event);
    }

    private boolean accept(StatusBarNotification sbn) {
        return sbn != null
                && ProbeStore.ZALO_PACKAGE.equals(sbn.getPackageName())
                && ProbeStore.isCaptureEnabled(this);
    }
}
