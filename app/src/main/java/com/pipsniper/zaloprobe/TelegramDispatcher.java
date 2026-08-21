package com.pipsniper.zaloprobe;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TelegramDispatcher {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final int MAX_BATCH = 100;

    private TelegramDispatcher() {}

    public static void kick(Context context) {
        Context app = context.getApplicationContext();
        if (!RUNNING.compareAndSet(false, true)) return;
        EXECUTOR.execute(() -> {
            try { drain(app); }
            finally { RUNNING.set(false); }
        });
    }

    private static void drain(Context c) {
        String token = SecureStore.getBotToken(c);
        if (token.isEmpty()) return;
        int processed = 0;
        while (processed < MAX_BATCH) {
            long now = System.currentTimeMillis();
            QueueItem item = QueueStore.firstDue(c, now);
            if (item == null) break;

            TelegramApi.Result result;
            if (item.hasMedia()) result = TelegramApi.sendPhoto(token, item.chatId, new File(item.mediaPath), item.text);
            else result = TelegramApi.sendText(token, item.chatId, item.text);

            if (result.ok) {
                QueueStore.remove(c, item.id);
                BridgePrefs.markForwarded(c);
                BridgePrefs.prefs(c).edit()
                        .putString("last_error", "")
                        .putLong("last_success_ms", System.currentTimeMillis())
                        .apply();
            } else {
                item.attempts++;
                long delay = result.retryAfterSeconds > 0
                        ? result.retryAfterSeconds * 1000L
                        : backoff(item.attempts);
                item.nextAttemptAt = System.currentTimeMillis() + delay;
                QueueStore.update(c, item);
                BridgePrefs.markFailed(c);
                BridgePrefs.prefs(c).edit()
                        .putString("last_error", safe(result.message))
                        .putLong("last_failure_ms", System.currentTimeMillis())
                        .apply();
                schedule(c, item.nextAttemptAt);
                break;
            }
            processed++;
        }

        long next = QueueStore.nextDueAt(c);
        if (next > 0L) schedule(c, Math.max(System.currentTimeMillis() + 1000L, next));
    }

    public static void schedule(Context c, long whenWallClock) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            Intent i = new Intent(c, RetryReceiver.class).setAction("com.pipsniper.zaloprobe.RETRY_FORWARD");
            PendingIntent pi = PendingIntent.getBroadcast(c, 701, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            long delay = Math.max(1000L, whenWallClock - System.currentTimeMillis());
            long triggerElapsed = SystemClock.elapsedRealtime() + delay;
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerElapsed, pi);
        } catch (Throwable ignored) {}
    }

    private static long backoff(int attempts) {
        if (attempts <= 1) return 5_000L;
        if (attempts == 2) return 20_000L;
        if (attempts == 3) return 60_000L;
        if (attempts == 4) return 5 * 60_000L;
        if (attempts == 5) return 15 * 60_000L;
        return 30 * 60_000L;
    }

    private static String safe(String s) {
        String x = s == null ? "" : s.trim();
        return x.length() <= 160 ? x : x.substring(0, 160);
    }
}
