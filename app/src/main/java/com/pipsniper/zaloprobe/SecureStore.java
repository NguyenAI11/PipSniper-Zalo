package com.pipsniper.zaloprobe;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class SecureStore {
    private static final String KEY_ALIAS = "pipsniper_bridge_token_v1";
    private static final String PREF_CIPHER = "telegram_token_cipher";
    private static final String PREF_IV = "telegram_token_iv";

    private SecureStore() {}

    private static SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        java.security.Key existing = ks.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        KeyGenerator gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        gen.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return gen.generateKey();
    }

    public static boolean saveBotToken(Context c, String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                clearBotToken(c);
                return true;
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key());
            byte[] encrypted = cipher.doFinal(token.trim().getBytes(StandardCharsets.UTF_8));
            BridgePrefs.prefs(c).edit()
                    .putString(PREF_CIPHER, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                    .putString(PREF_IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getBotToken(Context c) {
        try {
            String enc = BridgePrefs.prefs(c).getString(PREF_CIPHER, "");
            String iv = BridgePrefs.prefs(c).getString(PREF_IV, "");
            if (enc.isEmpty() || iv.isEmpty()) return "";
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
            byte[] clear = cipher.doFinal(Base64.decode(enc, Base64.NO_WRAP));
            return new String(clear, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean hasBotToken(Context c) {
        return !getBotToken(c).isEmpty();
    }

    public static void clearBotToken(Context c) {
        BridgePrefs.prefs(c).edit().remove(PREF_CIPHER).remove(PREF_IV).apply();
    }
}
