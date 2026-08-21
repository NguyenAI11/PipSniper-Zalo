package com.pipsniper.zaloprobe;

import android.content.Context;

import java.security.SecureRandom;

public final class BotSecurity {
    private static final String OWNER_ID = "smart_bot_owner_id";
    private static final String OWNER_CHAT_ID = "smart_bot_owner_chat_id";
    private static final String PAIR_CODE = "smart_bot_pair_code";
    private static final String PAIR_EXPIRES = "smart_bot_pair_expires";
    private static final long PAIR_TTL_MS = 30L * 60L * 1000L;
    private static final SecureRandom RNG = new SecureRandom();

    private BotSecurity() {}

    public static long ownerId(Context c) {
        return BridgePrefs.prefs(c).getLong(OWNER_ID, 0L);
    }

    public static long ownerChatId(Context c) {
        return BridgePrefs.prefs(c).getLong(OWNER_CHAT_ID, 0L);
    }

    public static boolean paired(Context c) {
        return ownerId(c) != 0L;
    }

    public static boolean isOwner(Context c, long userId) {
        return userId != 0L && userId == ownerId(c);
    }

    public static synchronized String ensurePairCode(Context c) {
        long now = System.currentTimeMillis();
        String current = BridgePrefs.prefs(c).getString(PAIR_CODE, "");
        long expires = BridgePrefs.prefs(c).getLong(PAIR_EXPIRES, 0L);
        if (!paired(c) && current != null && !current.isEmpty() && expires > now) return current;
        if (paired(c)) return "";
        return newPairCode(c);
    }

    public static synchronized String newPairCode(Context c) {
        if (paired(c)) return "";
        String code = String.format(java.util.Locale.US, "%06d", RNG.nextInt(1_000_000));
        BridgePrefs.prefs(c).edit()
                .putString(PAIR_CODE, code)
                .putLong(PAIR_EXPIRES, System.currentTimeMillis() + PAIR_TTL_MS)
                .apply();
        return code;
    }

    public static synchronized boolean pair(Context c, String code, long userId, long chatId) {
        if (paired(c)) return isOwner(c, userId);
        if (userId == 0L || chatId == 0L || code == null) return false;
        String expected = BridgePrefs.prefs(c).getString(PAIR_CODE, "");
        long expires = BridgePrefs.prefs(c).getLong(PAIR_EXPIRES, 0L);
        if (expected == null || expected.isEmpty() || System.currentTimeMillis() > expires) return false;
        if (!constantTimeEquals(expected, code.trim())) return false;
        BridgePrefs.prefs(c).edit()
                .putLong(OWNER_ID, userId)
                .putLong(OWNER_CHAT_ID, chatId)
                .remove(PAIR_CODE)
                .remove(PAIR_EXPIRES)
                .apply();
        return true;
    }

    public static synchronized void unpair(Context c) {
        BridgePrefs.prefs(c).edit()
                .remove(OWNER_ID)
                .remove(OWNER_CHAT_ID)
                .remove(PAIR_CODE)
                .remove(PAIR_EXPIRES)
                .apply();
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        int diff = a.length() ^ b.length();
        int n = Math.max(a.length(), b.length());
        for (int i = 0; i < n; i++) {
            char ca = i < a.length() ? a.charAt(i) : 0;
            char cb = i < b.length() ? b.charAt(i) : 0;
            diff |= ca ^ cb;
        }
        return diff == 0;
    }
}
