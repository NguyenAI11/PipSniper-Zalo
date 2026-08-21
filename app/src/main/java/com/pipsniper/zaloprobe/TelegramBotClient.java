package com.pipsniper.zaloprobe;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class TelegramBotClient {
    public static final class ApiResult {
        public boolean ok;
        public String error = "";
        public JSONObject root;

        public JSONObject resultObject() {
            return root == null ? null : root.optJSONObject("result");
        }

        public JSONArray resultArray() {
            return root == null ? null : root.optJSONArray("result");
        }
    }

    private TelegramBotClient() {}

    public static ApiResult getMe(String token) {
        return get(token, "getMe", "");
    }

    public static ApiResult getWebhookInfo(String token) {
        return get(token, "getWebhookInfo", "");
    }

    public static ApiResult getUpdates(String token, long offset, int timeoutSeconds) {
        try {
            String allowed = "[\"message\",\"edited_message\",\"channel_post\",\"edited_channel_post\",\"my_chat_member\",\"callback_query\"]";
            String q = "offset=" + offset
                    + "&timeout=" + Math.max(0, Math.min(25, timeoutSeconds))
                    + "&limit=100&allowed_updates=" + enc(allowed);
            return get(token, "getUpdates", q);
        } catch (Exception e) {
            return fail(e.getClass().getSimpleName());
        }
    }

    public static ApiResult getChatMember(String token, String chatId, long userId) {
        try {
            String q = "chat_id=" + enc(chatId) + "&user_id=" + userId;
            return get(token, "getChatMember", q);
        } catch (Exception e) {
            return fail(e.getClass().getSimpleName());
        }
    }

    public static ApiResult sendText(String token, String chatId, String text) {
        try {
            String body = "chat_id=" + enc(chatId) + "&text=" + enc(text)
                    + "&disable_web_page_preview=true";
            return postForm(token, "sendMessage", body);
        } catch (Exception e) {
            return fail(e.getClass().getSimpleName());
        }
    }

    public static ApiResult setDefaultCommands(String token) {
        try {
            JSONArray commands = new JSONArray();
            commands.put(command("start", "Khởi động / ghép chủ bot"));
            commands.put(command("help", "Danh sách lệnh"));
            commands.put(command("status", "Trạng thái Bridge"));
            commands.put(command("health", "Kiểm tra sức khỏe hệ thống"));
            commands.put(command("routes", "Các liên kết Zalo → Telegram"));
            commands.put(command("stats", "Thống kê chuyển tiếp"));
            commands.put(command("pause", "Tạm dừng chuyển tiếp"));
            commands.put(command("resume", "Bật lại chuyển tiếp"));
            commands.put(command("bridge", "Nhận diện group hiện tại"));
            commands.put(command("whereami", "Xem Chat ID hiện tại"));
            commands.put(command("test", "Kiểm tra bot"));
            String body = "commands=" + enc(commands.toString()) + "&language_code=vi";
            return postForm(token, "setMyCommands", body);
        } catch (Exception e) {
            return fail(e.getClass().getSimpleName());
        }
    }

    private static JSONObject command(String cmd, String description) throws Exception {
        JSONObject o = new JSONObject();
        o.put("command", cmd);
        o.put("description", description);
        return o;
    }

    private static ApiResult get(String token, String method, String query) {
        if (!validToken(token)) return fail("Bot Token không hợp lệ");
        HttpURLConnection con = null;
        try {
            String url = api(token, method) + (query == null || query.isEmpty() ? "" : "?" + query);
            con = open("GET", url);
            return parse(con);
        } catch (Exception e) {
            return fail(e.getClass().getSimpleName());
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private static ApiResult postForm(String token, String method, String body) {
        if (!validToken(token)) return fail("Bot Token không hợp lệ");
        HttpURLConnection con = null;
        try {
            con = open("POST", api(token, method));
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            con.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = con.getOutputStream()) { out.write(bytes); }
            return parse(con);
        } catch (Exception e) {
            return fail(e.getClass().getSimpleName());
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private static HttpURLConnection open(String method, String url) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod(method);
        con.setConnectTimeout(10_000);
        con.setReadTimeout(35_000);
        con.setUseCaches(false);
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("User-Agent", "PipSniper-Zalo-Bridge/1.2");
        return con;
    }

    private static ApiResult parse(HttpURLConnection con) throws Exception {
        int code = con.getResponseCode();
        InputStream raw = code >= 200 && code < 400 ? con.getInputStream() : con.getErrorStream();
        String body = raw == null ? "{}" : readAll(raw);
        JSONObject root;
        try { root = new JSONObject(body); }
        catch (Exception e) { root = new JSONObject(); }
        ApiResult r = new ApiResult();
        r.root = root;
        r.ok = root.optBoolean("ok", code >= 200 && code < 300);
        if (!r.ok) r.error = root.optString("description", "HTTP " + code);
        return r;
    }

    private static ApiResult fail(String error) {
        ApiResult r = new ApiResult();
        r.ok = false;
        r.error = error == null ? "Unknown error" : error;
        r.root = new JSONObject();
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

    private static boolean validToken(String token) {
        return token != null && token.matches("^[0-9]{5,}:[A-Za-z0-9_-]{20,}$");
    }

    private static String api(String token, String method) {
        return "https://api.telegram.org/bot" + token.trim() + "/" + method;
    }

    private static String enc(String s) throws Exception {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8.name());
    }
}
