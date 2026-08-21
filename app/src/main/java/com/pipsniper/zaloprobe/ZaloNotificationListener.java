package com.pipsniper.zaloprobe;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class ZaloNotificationListener extends NotificationListenerService {
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        BridgePrefs.setListenerConnected(this, true);
        RouteStore.ensureSeedRoutes(this);
        TelegramDispatcher.kick(this);
    }

    @Override
    public void onListenerDisconnected() {
        BridgePrefs.setListenerConnected(this, false);
        try { requestRebind(new android.content.ComponentName(this, ZaloNotificationListener.class)); }
        catch (Throwable ignored) {}
        super.onListenerDisconnected();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !BridgePrefs.ZALO_PACKAGE.equals(sbn.getPackageName())) return;
        BridgeEngine.onPosted(this, sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap, int reason) {
        if (sbn == null || !BridgePrefs.ZALO_PACKAGE.equals(sbn.getPackageName())) return;
        BridgeEngine.onRemoved(this, sbn);
    }
}
