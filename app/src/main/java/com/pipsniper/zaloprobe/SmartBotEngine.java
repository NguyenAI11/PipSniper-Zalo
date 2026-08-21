package com.pipsniper.zaloprobe;

import android.content.Context;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class SmartBotEngine {
    private SmartBotEngine() {}

    public static void handleUpdate(Context c, String token, JSONObject update) {
        if (c == null || update == null) return;
        JSONObject message = update.optJSONObject("message");
        if (message == null) return;

        JSONObject chat = message.optJSONObject("chat");
        if (chat == null) return;
        long chatId = chat.optLong("id", 0L);
        String chatType = chat.optString("type", "");
        String title = chat.optString("title", "");
        String text = message.optString("text", "").trim();
        JSONObject from = message.optJSONObject("from");
        long userId = from == null ? 0L : from.optLong("id", 0L);
        boolean isPrivate = "private".equals(chatType);

        rememberChat(c, chat);
        if (text.isEmpty()) return;

        String normalized = normalize(text);

        if (!BotSecurity.paired(c)) {
            if (normalized.startsWith("/pair ")) {
                if (!isPrivate) {
                    reply(token, chatId, "🔐 Hãy gửi lệnh /pair trong chat riêng với bot.");
                    return;
                }
                String code = normalized.substring(6).trim();
                if (BotSecurity.pair(c, code, userId, chatId)) {
                    TelegramBotClient.setDefaultCommands(token);
                    reply(token, chatId,
                            "✅ Ghép chủ bot thành công.\n"
                            + "Tài khoản Telegram này hiện là Owner của PipSniper.\n\n"
                            + "Gửi /status để kiểm tra hệ thống hoặc /help để xem lệnh.");
                } else {
                    reply(token, chatId, "❌ Mã ghép không đúng hoặc đã hết hạn. Hãy tạo mã mới trong app PipSniper.");
                }
                return;
            }

            if (normalized.startsWith("/bridge") && !isPrivate) {
                reply(token, chatId, "✅ PipSniper đã nhận diện group này. Hãy ghép Owner trong app để bật điều khiển thông minh.");
                return;
            }

            if (normalized.startsWith("/start") || normalized.startsWith("/help")) {
                reply(token, chatId,
                        "👋 PipSniper Smart Bot đã hoạt động.\n\n"
                        + "Để bảo vệ hệ thống, bot cần ghép Owner một lần.\n"
                        + "Mở PipSniper Zalo Bridge → Smart Bot → tạo mã ghép, sau đó gửi:\n"
                        + "/pair 123456");
            }
            return;
        }

        if (!BotSecurity.isOwner(c, userId)) {
            if (normalized.startsWith("/bridge") && !isPrivate) {
                reply(token, chatId, "✅ PipSniper đã nhận diện group này.");
            }
            return;
        }

        String intent = intent(normalized);
        switch (intent) {
            case "start":
            case "help":
                reply(token, chatId, helpText());
                break;
            case "status":
                reply(token, chatId, statusText(c));
                break;
            case "health":
                reply(token, chatId, healthText(c, token));
                break;
            case "routes":
                reply(token, chatId, routesText(c));
                break;
            case "stats":
                reply(token, chatId, statsText(c));
                break;
            case "pause":
                BridgePrefs.setEnabled(c, false);
                reply(token, chatId, "⏸ Đã tạm dừng Zalo → Telegram. Queue vẫn được giữ nguyên.");
                break;
            case "resume":
                BridgePrefs.setEnabled(c, true);
                TelegramDispatcher.kick(c);
                reply(token, chatId, "▶️ Đã bật lại Zalo → Telegram.");
                break;
            case "bridge":
                reply(token, chatId, bridgeText(c, token, chat));
                break;
            case "whereami":
                reply(token, chatId, "📍 Chat hiện tại\nTên: " + safeTitle(chat) + "\nType: " + chatType + "\nChat ID: " + chatId);
                break;
            case "test":
                reply(token, chatId, "✅ Smart Bot đang online và nhận lệnh bình thường.\n" + shortStatus(c));
                break;
            default:
                if (isPrivate) reply(token, chatId, "Tôi chưa hiểu yêu cầu này. Gửi /help để xem các lệnh hỗ trợ.");
                break;
        }
    }

    private static void rememberChat(Context c, JSONObject chat) {
        String type = chat.optString("type", "");
        if (!("group".equals(type) || "supergroup".equals(type) || "channel".equals(type))) return;
        long id = chat.optLong("id", 0L);
        if (id == 0L) return;
        TelegramDiscovery.ChatCandidate x = new TelegramDiscovery.ChatCandidate();
        x.id = id;
        x.title = chat.optString("title", "");
        x.type = type;
        x.username = chat.optString("username", "");
        List<TelegramDiscovery.ChatCandidate> one = new ArrayList<>();
        one.add(x);
        TelegramChatStore.merge(c, one);
    }

    private static String intent(String n) {
        if (n.startsWith("/start")) return "start";
        if (n.startsWith("/help")) return "help";
        if (n.startsWith("/status")) return "status";
        if (n.startsWith("/health")) return "health";
        if (n.startsWith("/routes")) return "routes";
        if (n.startsWith("/stats")) return "stats";
        if (n.startsWith("/pause")) return "pause";
        if (n.startsWith("/resume")) return "resume";
        if (n.startsWith("/bridge")) return "bridge";
        if (n.startsWith("/whereami")) return "whereami";
        if (n.startsWith("/test")) return "test";

        if (equalsAny(n, "trạng thái", "trang thai", "status", "bridge thế nào", "bridge the nao")) return "status";
        if (containsAny(n, "kiểm tra hệ thống", "kiem tra he thong", "health check", "sức khỏe hệ thống", "suc khoe he thong")) return "health";
        if (equalsAny(n, "các nhóm", "cac nhom", "các route", "cac route", "liên kết", "lien ket")) return "routes";
        if (containsAny(n, "thống kê", "thong ke")) return "stats";
        if (equalsAny(n, "dừng bridge", "dung bridge", "tạm dừng bridge", "tam dung bridge")) return "pause";
        if (equalsAny(n, "bật bridge", "bat bridge", "tiếp tục bridge", "tiep tuc bridge")) return "resume";
        return "unknown";
    }

    private static String helpText() {
        return "🤖 PipSniper Smart Bot\n\n"
                + "/status — trạng thái Bridge\n"
                + "/health — kiểm tra hệ thống\n"
                + "/routes — các liên kết Zalo → Telegram\n"
                + "/stats — thống kê chuyển tiếp\n"
                + "/pause — tạm dừng Bridge\n"
                + "/resume — bật lại Bridge\n"
                + "/bridge — nhận diện/kiểm tra group hiện tại\n"
                + "/whereami — xem Chat ID\n"
                + "/test — kiểm tra bot\n\n"
                + "Bot cũng hiểu một số câu tiếng Việt như “trạng thái”, “kiểm tra hệ thống”, “dừng bridge”, “bật bridge”.";
    }

    private static String statusText(Context c) {
        boolean listener = BridgePrefs.listenerConnected(c);
        boolean enabled = BridgePrefs.enabled(c);
        int readyRoutes = 0;
        for (Route r : RouteStore.load(c)) if (r.ready()) readyRoutes++;
        return "📊 TRẠNG THÁI PIPSNIPER\n"
                + "Bridge: " + (enabled ? "🟢 ĐANG CHẠY" : "🟠 TẠM DỪNG") + "\n"
                + "Zalo Listener: " + (listener ? "🟢 Kết nối" : "🔴 Mất kết nối") + "\n"
                + "Route hoạt động: " + readyRoutes + "\n"
                + "Queue chờ: " + QueueStore.size(c) + "\n"
                + "Forward thành công: " + BridgePrefs.prefs(c).getLong("forwarded", 0L) + "\n"
                + "Lỗi/retry: " + BridgePrefs.prefs(c).getLong("failed", 0L) + lastErrorLine(c);
    }

    private static String shortStatus(Context c) {
        return "Bridge " + (BridgePrefs.enabled(c) ? "ON" : "OFF")
                + " • Listener " + (BridgePrefs.listenerConnected(c) ? "OK" : "OFF")
                + " • Queue " + QueueStore.size(c);
    }

    private static String healthText(Context c, String token) {
        StringBuilder s = new StringBuilder("🩺 HEALTH CHECK\n");
        TelegramBotClient.ApiResult me = TelegramBotClient.getMe(token);
        s.append("Telegram API: ").append(me.ok ? "✅ OK" : "❌ " + me.error).append('\n');
        s.append("Zalo Listener: ").append(BridgePrefs.listenerConnected(c) ? "✅ OK" : "❌ DISCONNECTED").append('\n');
        s.append("Bridge: ").append(BridgePrefs.enabled(c) ? "✅ ON" : "⚠️ PAUSED").append('\n');
        int ready = 0;
        for (Route r : RouteStore.load(c)) if (r.ready()) ready++;
        s.append("Routes: ").append(ready > 0 ? "✅ " : "⚠️ ").append(ready).append(" hoạt động\n");
        int q = QueueStore.size(c);
        s.append("Queue: ").append(q == 0 ? "✅ 0" : (q < 20 ? "⚠️ " + q : "❌ " + q));
        String err = BridgePrefs.prefs(c).getString("last_error", "");
        if (err != null && !err.trim().isEmpty()) s.append("\nLỗi gần nhất: ").append(err.trim());
        return s.toString();
    }

    private static String routesText(Context c) {
        List<Route> routes = RouteStore.load(c);
        if (routes.isEmpty()) return "🧭 Chưa có liên kết Zalo → Telegram.";
        StringBuilder s = new StringBuilder("🧭 LIÊN KẾT HIỆN TẠI\n");
        int i = 1;
        for (Route r : routes) {
            s.append('\n').append(i++).append(". ").append(r.enabled ? "🟢 " : "⚪ ")
                    .append(r.label == null ? "Chuyên gia" : r.label)
                    .append("\n   Telegram: ")
                    .append(r.telegramChat == null || r.telegramChat.trim().isEmpty() ? "chưa chọn" : r.telegramChat.trim());
            if (i > 12) { s.append("\n…"); break; }
        }
        return s.toString();
    }

    private static String statsText(Context c) {
        long seen = BridgePrefs.prefs(c).getLong("zalo_seen", 0L);
        long forwarded = BridgePrefs.prefs(c).getLong("forwarded", 0L);
        long failed = BridgePrefs.prefs(c).getLong("failed", 0L);
        long lastForward = BridgePrefs.prefs(c).getLong("last_forward_ms", 0L);
        return "📈 THỐNG KÊ\n"
                + "Zalo notifications: " + seen + "\n"
                + "Đã forward: " + forwarded + "\n"
                + "Lỗi/retry: " + failed + "\n"
                + "Queue: " + QueueStore.size(c)
                + (lastForward > 0 ? "\nForward gần nhất: " + time(lastForward) : "");
    }

    private static String bridgeText(Context c, String token, JSONObject chat) {
        long chatId = chat.optLong("id", 0L);
        StringBuilder s = new StringBuilder();
        s.append("✅ Đã nhận diện group\nTên: ").append(safeTitle(chat))
                .append("\nChat ID: ").append(chatId);
        TelegramBotClient.ApiResult me = TelegramBotClient.getMe(token);
        JSONObject meObj = me.resultObject();
        if (me.ok && meObj != null) {
            long botId = meObj.optLong("id", 0L);
            TelegramBotClient.ApiResult member = TelegramBotClient.getChatMember(token, String.valueOf(chatId), botId);
            JSONObject m = member.resultObject();
            if (member.ok && m != null) {
                String status = m.optString("status", "unknown");
                s.append("\nBot role: ").append(status);
                if ("administrator".equals(status)) s.append(" ✅");
            } else if (!member.ok) {
                s.append("\nQuyền bot: ⚠️ ").append(member.error);
            }
        }
        return s.toString();
    }

    private static String lastErrorLine(Context c) {
        String err = BridgePrefs.prefs(c).getString("last_error", "");
        return err == null || err.trim().isEmpty() ? "" : "\nLỗi gần nhất: " + err.trim();
    }

    private static String normalize(String text) {
        String n = text.trim().toLowerCase(Locale.ROOT);
        int space = n.indexOf(' ');
        String first = space < 0 ? n : n.substring(0, space);
        if (first.startsWith("/") && first.contains("@")) {
            first = first.substring(0, first.indexOf('@'));
            n = first + (space < 0 ? "" : n.substring(space));
        }
        return n.trim();
    }

    private static boolean equalsAny(String n, String... options) {
        for (String x : options) if (n.equals(x)) return true;
        return false;
    }

    private static boolean containsAny(String n, String... options) {
        for (String x : options) if (n.contains(x)) return true;
        return false;
    }

    private static String safeTitle(JSONObject chat) {
        String title = chat.optString("title", "");
        if (!title.trim().isEmpty()) return title.trim();
        String username = chat.optString("username", "");
        if (!username.trim().isEmpty()) return "@" + username.trim();
        String first = chat.optString("first_name", "");
        if (!first.trim().isEmpty()) return first.trim();
        return String.valueOf(chat.optLong("id", 0L));
    }

    private static void reply(String token, long chatId, String text) {
        if (chatId == 0L || text == null || text.isEmpty()) return;
        TelegramBotClient.sendText(token, String.valueOf(chatId), text);
    }

    private static String time(long ms) {
        return new SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(new Date(ms));
    }
}
