package com.pipsniper.zaloprobe;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class TelegramApi {
    public static final class Result {
        public boolean ok;
        public String message = "";
        public int retryAfterSeconds;

        public static Result ok(String message) {
            Result r = new Result(); r.ok = true; r.message = message; return r;
        }

        public static Result fail(String message) {
            Result r = new Result(); r.ok = false; r.message = message; return r;
        }
    }

    private TelegramApi() {}

    public static Result testBot(String token) {
        if (!validToken(token)) return Result.fail("Bot Token không hợp lệ");
        return requestJson("GET", api(token, "getMe"), null, null);
    }

    public static Result testChat(String token, String chatId) {
        if (!validToken(token)) return Result.fail("Bot Token không hợp lệ");
        if (blank(chatId)) return Result.fail("Chưa nhập Telegram Chat ID");
        try {
            String u = api(token, "getChat") + "?chat_id=" + enc(chatId.trim());
            return requestJson("GET", u, null, null);
        } catch (Exception e) {
            return Result.fail(e.getClass().getSimpleName());
        }
    }

    public static Result sendText(String token, String chatId, String text) {
        if (!validToken(token)) return Result.fail("Bot Token không hợp lệ");
        if (blank(chatId)) return Result.fail("Telegram Chat ID trống");
        if (blank(text)) return Result.ok("EMPTY_SKIPPED");
        try {
            String body = "chat_id=" + enc(chatId.trim()) + "&text=" + enc(text);
            return requestJson("POST", api(token, "sendMessage"), "application/x-www-form-urlencoded; charset=UTF-8", body.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return Result.fail(e.getClass().getSimpleName());
        }
    }

    public static Result sendPhoto(String token, String chatId, File image, String caption) {
        if (!validToken(token)) return Result.fail("Bot Token không hợp lệ");
        if (blank(chatId)) return Result.fail("Telegram Chat ID trống");
        if (image == null || !image.isFile()) return Result.fail("Media file missing");
        HttpURLConnection con = null;
        try {
            String boundary = "----PipSniperBoundary" + System.nanoTime();
            con = open("POST", api(token, "sendPhoto"));
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            con.setDoOutput(true);
            try (OutputStream out = con.getOutputStream()) {
                writeField(out, boundary, "chat_id", chatId.trim());
                if (!blank(caption)) writeField(out, boundary, "caption", caption.length() > 1000 ? caption.substring(0, 1000) : caption);
                String head = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"photo\"; filename=\"zalo.png\"\r\nContent-Type: image/png\r\n\r\n";
                out.write(head.getBytes(StandardCharsets.UTF_8));
                try (InputStream in = new BufferedInputStream(new FileInputStream(image))) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
                }
                out.write("\r\n".getBytes(StandardCharsets.UTF_8));
                out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            }
            return parseResponse(con);
        } catch (Exception e) {
            return Result.fail(e.getClass().getSimpleName());
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private static Result requestJson(String method, String url, String contentType, byte[] body) {
        HttpURLConnection con = null;
        try {
            con = open(method, url);
            if (body != null) {
                con.setDoOutput(true);
                if (contentType != null) con.setRequestProperty("Content-Type", contentType);
                try (OutputStream out = con.getOutputStream()) { out.write(body); }
            }
            return parseResponse(con);
        } catch (Exception e) {
            return Result.fail(e.getClass().getSimpleName());
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private static HttpURLConnection open(String method, String url) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod(method);
        con.setConnectTimeout(10_000);
        con.setReadTimeout(20_000);
        con.setUseCaches(false);
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("User-Agent", "PipSniper-Zalo-Bridge/1.0");
        return con;
    }

    private static Result parseResponse(HttpURLConnection con) throws Exception {
        int code = con.getResponseCode();
        InputStream raw = code >= 200 && code < 400 ? con.getInputStream() : con.getErrorStream();
        String body = raw == null ? "" : readAll(raw);
        Result r = new Result();
        try {
            JSONObject o = new JSONObject(body);
            r.ok = o.optBoolean("ok", false);
            r.message = r.ok ? "OK" : o.optString("description", "HTTP " + code);
            JSONObject p = o.optJSONObject("parameters");
            if (p != null) r.retryAfterSeconds = p.optInt("retry_after", 0);
        } catch (Exception e) {
            r.ok = code >= 200 && code < 300;
            r.message = r.ok ? "OK" : "HTTP " + code;
        }
        return r;
    }

    private static String readAll(InputStream in) throws Exception {
        try (InputStream x = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = x.read(buf)) >= 0) out.write(buf, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static void writeField(OutputStream out, String boundary, String name, String value) throws Exception {
        String s = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n";
        out.write(s.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean validToken(String token) {
        return token != null && token.matches("^[0-9]{5,}:[A-Za-z0-9_-]{20,}$");
    }

    private static boolean blank(String s) { return s == null || s.trim().isEmpty(); }
    private static String api(String token, String method) { return "https://api.telegram.org/bot" + token.trim() + "/" + method; }
    private static String enc(String s) throws Exception { return URLEncoder.encode(s, StandardCharsets.UTF_8.name()); }
}
