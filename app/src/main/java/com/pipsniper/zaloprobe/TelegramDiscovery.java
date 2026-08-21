package com.pipsniper.zaloprobe;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TelegramDiscovery {
    public static final class BotProfile {
        public boolean ok;
        public long id;
        public String username = "";
        public String firstName = "";
        public String error = "";
    }

    public static final class ChatCandidate {
        public long id;
        public String title = "";
        public String type = "";
        public String username = "";

        public String target() { return String.valueOf(id); }
        public String displayName() {
            if (title != null && !title.trim().isEmpty()) return title.trim();
            if (username != null && !username.trim().isEmpty()) return "@" + username.trim();
            return String.valueOf(id);
        }
    }

    public static final class ScanResult {
        public boolean ok;
        public String message = "";
        public BotProfile bot;
        public final List<ChatCandidate> chats = new ArrayList<>();
        public String webhookUrl = "";
        public int pendingUpdates;
    }

    private TelegramDiscovery() {}

    public static BotProfile profile(String token) {
        BotProfile p = new BotProfile();
        if (!validToken(token)) {
            p.error = "Bot Token không hợp lệ";
            return p;
        }
        try {
            JSONObject root = get(api(token, "getMe"));
            p.ok = root.optBoolean("ok", false);
            if (!p.ok) {
                p.error = root.optString("description", "getMe failed");
                return p;
            }
            JSONObject r = root.optJSONObject("result");
            if (r != null) {
                p.id = r.optLong("id", 0L);
                p.username = r.optString("username", "");
                p.firstName = r.optString("first_name", "");
            }
            return p;
        } catch (Exception e) {
            p.error = e.getClass().getSimpleName();
            return p;
        }
    }

    public static ScanResult scanChats(String token) {
        ScanResult out = new ScanResult();
        out.bot = profile(token);
        if (out.bot == null || !out.bot.ok) {
            out.message = out.bot == null ? "Không đọc được bot" : out.bot.error;
            return out;
        }
        try {
            TelegramBotClient.ApiResult wh = TelegramBotClient.getWebhookInfo(token);
            JSONObject wr = wh.resultObject();
            if (wh.ok && wr != null) {
                out.webhookUrl = wr.optString("url", "");
                out.pendingUpdates = wr.optInt("pending_update_count", 0);
            }
            if (out.webhookUrl != null && !out.webhookUrl.isEmpty()) {
                out.message = "Bot đang dùng webhook nên Smart Bot/getUpdates không thể hoạt động.";
                return out;
            }

            // Peek at currently unconfirmed updates through the same serialized client
            // used by Smart Bot. Do not advance the offset here: the runtime owns
            // confirmation and command processing, preventing discovery from eating commands.
            TelegramBotClient.ApiResult updates = TelegramBotClient.getUpdates(token, 0L, 0);
            if (!updates.ok) {
                out.message = updates.error == null || updates.error.isEmpty() ? "getUpdates failed" : updates.error;
                return out;
            }

            JSONArray arr = updates.resultArray();
            Map<Long, ChatCandidate> unique = new LinkedHashMap<>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject update = arr.optJSONObject(i);
                    if (update == null) continue;
                    collectChat(update.optJSONObject("message"), unique);
                    collectChat(update.optJSONObject("edited_message"), unique);
                    collectChat(update.optJSONObject("channel_post"), unique);
                    collectChat(update.optJSONObject("edited_channel_post"), unique);
                    JSONObject member = update.optJSONObject("my_chat_member");
                    if (member != null) collectChatObject(member.optJSONObject("chat"), unique);
                }
            }
            out.chats.addAll(unique.values());
            out.ok = true;
            out.message = out.chats.isEmpty() ? "Chưa phát hiện group/channel mới. Các group Smart Bot đã nhận trước đó vẫn được giữ trong bộ nhớ app." : "OK";
            return out;
        } catch (Exception e) {
            out.message = e.getClass().getSimpleName();
            return out;
        }
    }

    private static void collectChat(JSONObject message, Map<Long, ChatCandidate> out) {
        if (message == null) return;
        collectChatObject(message.optJSONObject("chat"), out);
    }

    private static void collectChatObject(JSONObject chat, Map<Long, ChatCandidate> out) {
        if (chat == null) return;
        String type = chat.optString("type", "");
        if (!("group".equals(type) || "supergroup".equals(type) || "channel".equals(type))) return;
        long id = chat.optLong("id", 0L);
        if (id == 0L) return;
        ChatCandidate c = new ChatCandidate();
        c.id = id;
        c.type = type;
        c.title = chat.optString("title", "");
        c.username = chat.optString("username", "");
        out.put(id, c);
    }

    private static JSONObject get(String url) throws Exception {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(10_000);
            con.setReadTimeout(20_000);
            con.setUseCaches(false);
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("User-Agent", "PipSniper-Zalo-Bridge/1.2");
            int code = con.getResponseCode();
            InputStream raw = code >= 200 && code < 400 ? con.getInputStream() : con.getErrorStream();
            String body = raw == null ? "{}" : readAll(raw);
            JSONObject o = new JSONObject(body);
            if (code < 200 || code >= 400) {
                if (!o.has("description")) o.put("description", "HTTP " + code);
            }
            return o;
        } finally {
            if (con != null) con.disconnect();
        }
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
}
