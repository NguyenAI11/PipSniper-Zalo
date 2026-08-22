package com.pipsniper.zaloprobe;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

public final class TelegramBotRuntime {
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static volatile Thread worker;
    private static final String OFFSET_KEY = "smart_bot_update_offset";
    private static final String RUNTIME_OK_KEY = "smart_bot_runtime_ok";
    private static final String RUNTIME_TOUCH_KEY = "smart_bot_runtime_touch_ms";
    private static final String RUNTIME_ERROR_KEY = "smart_bot_runtime_error";

    private TelegramBotRuntime() {}

    public static void start(Context context) {
        if (context == null || !RUNNING.compareAndSet(false, true)) return;
        Context app = context.getApplicationContext();
        worker = new Thread(() -> runLoop(app), "PipSniper-TelegramBot");
        worker.setDaemon(true);
        worker.start();
    }

    public static void stop() {
        RUNNING.set(false);
        Thread t = worker;
        if (t != null) t.interrupt();
        worker = null;
    }

    public static boolean running() {
        return RUNNING.get();
    }

    private static void runLoop(Context c) {
        long lastWebhookCheck = 0L;
        boolean webhookClear = false;
        try {
            while (RUNNING.get()) {
                String token = SecureStore.getBotToken(c);
                if (token == null || token.isEmpty()) {
                    mark(c, false, "Chưa có Bot Token");
                    sleep(3000L);
                    continue;
                }

                long now = System.currentTimeMillis();
                if (!webhookClear || now - lastWebhookCheck > 60_000L) {
                    TelegramBotClient.ApiResult wh = TelegramBotClient.getWebhookInfo(token);
                    lastWebhookCheck = now;
                    JSONObject wr = wh.resultObject();
                    String url = wr == null ? "" : wr.optString("url", "");
                    webhookClear = wh.ok && (url == null || url.isEmpty());
                    if (!webhookClear) {
                        String reason = !wh.ok ? wh.error : "Bot đang dùng webhook; Smart Bot cần getUpdates.";
                        mark(c, false, reason);
                        sleep(5000L);
                        continue;
                    }
                }

                long offset = BridgePrefs.prefs(c).getLong(OFFSET_KEY, 0L);
                TelegramBotClient.ApiResult result = TelegramBotClient.getUpdates(token, offset, 20);
                if (!result.ok) {
                    mark(c, false, result.error);
                    if (result.error != null && result.error.toLowerCase(java.util.Locale.ROOT).contains("webhook")) webhookClear = false;
                    sleep(2500L);
                    continue;
                }

                mark(c, true, "");
                JSONArray arr = result.resultArray();
                if (arr == null || arr.length() == 0) continue;

                for (int i = 0; i < arr.length() && RUNNING.get(); i++) {
                    JSONObject update = arr.optJSONObject(i);
                    if (update == null) continue;
                    long updateId = update.optLong("update_id", -1L);
                    try {
                        SmartBotEngine.handleUpdate(c, token, update);
                    } catch (Throwable t) {
                        BridgePrefs.prefs(c).edit().putString(RUNTIME_ERROR_KEY, "handle: " + t.getClass().getSimpleName()).apply();
                    } finally {
                        if (updateId >= 0L) {
                            BridgePrefs.prefs(c).edit().putLong(OFFSET_KEY, updateId + 1L).apply();
                        }
                    }
                }
            }
        } finally {
            RUNNING.set(false);
            mark(c, false, "Runtime stopped");
        }
    }

    private static void mark(Context c, boolean ok, String error) {
        BridgePrefs.prefs(c).edit()
                .putBoolean(RUNTIME_OK_KEY, ok)
                .putLong(RUNTIME_TOUCH_KEY, System.currentTimeMillis())
                .putString(RUNTIME_ERROR_KEY, error == null ? "" : error)
                .apply();
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
