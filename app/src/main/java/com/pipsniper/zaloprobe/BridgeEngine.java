package com.pipsniper.zaloprobe;

import android.content.Context;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.List;

public final class BridgeEngine {
    private BridgeEngine() {}

    public static void onPosted(Context context, StatusBarNotification sbn) {
        if (context == null || sbn == null) return;
        if (!BridgePrefs.ZALO_PACKAGE.equals(sbn.getPackageName())) return;
        Context c = context.getApplicationContext();

        NotificationParser.Payload payload = NotificationParser.parse(sbn);
        RecentSourceStore.touch(c, sbn, payload);
        BridgePrefs.markSeen(c);
        if (!BridgePrefs.enabled(c)) return;

        String keyHash = Hashing.shortHash(sbn.getKey());
        List<Route> routes = RouteStore.findAllMatching(c, sbn.getId(), keyHash);
        if (routes.isEmpty()) return;

        boolean queued = false;
        for (Route route : routes) {
            DedupStore.Delta delta = DedupStore.diff(c, route, payload);
            for (String line : delta.newLines) {
                for (String part : splitForTelegram(line)) {
                    QueueItem item = new QueueItem();
                    item.routeId = route.id;
                    item.chatId = route.telegramChat.trim();
                    item.text = part;
                    item.contentHash = Hashing.sha256("text|" + item.id + "|" + part);
                    if (QueueStore.enqueue(c, item)) queued = true;
                    else markQueueFailure(c, "Queue đầy hoặc không thể lưu text");
                }
            }

            if (delta.newImage && payload.imagePng != null && payload.imagePng.length > 0) {
                QueueItem item = new QueueItem();
                item.routeId = route.id;
                item.chatId = route.telegramChat.trim();
                item.contentHash = Hashing.sha256("image|" + item.id + "|" + Hashing.sha256(payload.imagePng));
                try {
                    item.mediaPath = QueueStore.saveMedia(c, item.id, payload.imagePng);
                    if (QueueStore.enqueue(c, item)) queued = true;
                    else markQueueFailure(c, "Queue đầy hoặc không thể lưu ảnh");
                } catch (Exception e) {
                    markQueueFailure(c, "Không lưu được ảnh Zalo");
                }
            }
        }

        if (queued) TelegramDispatcher.kick(c);
    }

    public static void onRemoved(Context context, StatusBarNotification sbn) {
        if (context == null || sbn == null) return;
        if (!BridgePrefs.ZALO_PACKAGE.equals(sbn.getPackageName())) return;
        Context c = context.getApplicationContext();
        String keyHash = Hashing.shortHash(sbn.getKey());
        for (Route route : RouteStore.findAllMatching(c, sbn.getId(), keyHash)) DedupStore.markRemoved(c, route);
    }

    public static List<String> splitForTelegram(String input) {
        final int max = 3900;
        List<String> out = new ArrayList<>();
        String text = Hashing.normalizeText(input);
        while (text.length() > max) {
            int cut = text.lastIndexOf('\n', max);
            if (cut < max / 2) cut = text.lastIndexOf(' ', max);
            if (cut < max / 2) cut = max;
            String part = text.substring(0, cut).trim();
            if (!part.isEmpty()) out.add(part);
            text = text.substring(Math.min(text.length(), cut)).trim();
        }
        if (!text.isEmpty()) out.add(text);
        return out;
    }

    private static void markQueueFailure(Context c, String message) {
        BridgePrefs.markFailed(c);
        BridgePrefs.prefs(c).edit().putString("last_error", message).apply();
    }
}
