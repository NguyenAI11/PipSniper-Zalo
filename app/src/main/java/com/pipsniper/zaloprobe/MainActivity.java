package com.pipsniper.zaloprobe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView listenerStatus;
    private TextView telegramStatus;
    private TextView botIdentity;
    private TextView bridgeStatus;
    private TextView statsText;
    private LinearLayout routesBox;
    private EditText tokenInput;
    private final android.os.Handler handler = new android.os.Handler();

    private final Runnable refreshLoop = new Runnable() {
        @Override public void run() {
            refreshStatus();
            handler.postDelayed(this, 1500);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("PipSniper Zalo Bridge");
        RouteStore.ensureSeedRoutes(this);
        setContentView(buildUi());
        TelegramDispatcher.kick(this);
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(refreshLoop);
        handler.post(refreshLoop);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(refreshLoop);
        super.onPause();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(245, 247, 250));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(34));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        root.addView(text("PipSniper Zalo Bridge", 26, Color.rgb(15, 23, 42), Typeface.BOLD));
        TextView subtitle = text("Zalo → Telegram • chuyển nguyên văn • không AI", 14, Color.rgb(71, 85, 105), Typeface.NORMAL);
        subtitle.setPadding(0, dp(5), 0, dp(14));
        root.addView(subtitle);

        LinearLayout master = card();
        master.addView(text("Trạng thái Bridge", 17, Color.rgb(15, 23, 42), Typeface.BOLD));
        bridgeStatus = text("", 14, Color.rgb(71, 85, 105), Typeface.NORMAL);
        bridgeStatus.setPadding(0, dp(8), 0, dp(10));
        master.addView(bridgeStatus);
        Button toggle = secondaryButton("BẬT / TẮT CHUYỂN TIẾP");
        toggle.setOnClickListener(v -> {
            BridgePrefs.setEnabled(this, !BridgePrefs.enabled(this));
            if (BridgePrefs.enabled(this)) TelegramDispatcher.kick(this);
            refreshStatus();
        });
        master.addView(toggle);
        root.addView(master, lp(-1, -2, 0, 0, 0, 12));

        LinearLayout zalo = card();
        zalo.addView(text("1. Kết nối Zalo", 17, Color.rgb(15, 23, 42), Typeface.BOLD));
        listenerStatus = text("Đang kiểm tra...", 14, Color.rgb(71, 85, 105), Typeface.NORMAL);
        listenerStatus.setPadding(0, dp(8), 0, dp(10));
        zalo.addView(listenerStatus);
        Button access = primaryButton("CẤP / KIỂM TRA NOTIFICATION ACCESS");
        access.setOnClickListener(v -> openNotificationAccess());
        zalo.addView(access);
        root.addView(zalo, lp(-1, -2, 0, 0, 0, 12));

        LinearLayout telegram = card();
        telegram.addView(text("2. Kết nối Telegram", 17, Color.rgb(15, 23, 42), Typeface.BOLD));
        telegramStatus = text("", 14, Color.rgb(71, 85, 105), Typeface.NORMAL);
        telegramStatus.setPadding(0, dp(8), 0, dp(4));
        telegram.addView(telegramStatus);
        botIdentity = text("", 13, Color.rgb(51, 65, 85), Typeface.NORMAL);
        botIdentity.setPadding(0, 0, 0, dp(8));
        telegram.addView(botIdentity);
        tokenInput = new EditText(this);
        tokenInput.setHint(SecureStore.hasBotToken(this) ? "Bot Token đã lưu — nhập mới để thay" : "Telegram Bot Token");
        tokenInput.setSingleLine(true);
        tokenInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tokenInput.setTextSize(14);
        tokenInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        tokenInput.setBackground(roundRect(Color.rgb(248, 250, 252), 12));
        telegram.addView(tokenInput, lp(-1, dp(50), 0, 0, 0, 10));
        Button test = primaryButton("LƯU & KIỂM TRA TELEGRAM");
        test.setOnClickListener(v -> testTelegram());
        telegram.addView(test);
        Button scanTg = secondaryButton("QUÉT NHÓM TELEGRAM");
        scanTg.setOnClickListener(v -> scanTelegramChats(null));
        telegram.addView(scanTg, lp(-1, dp(46), 0, 8, 0, 0));
        root.addView(telegram, lp(-1, -2, 0, 0, 0, 12));

        LinearLayout routes = card();
        LinearLayout routeHeader = new LinearLayout(this);
        routeHeader.setOrientation(LinearLayout.HORIZONTAL);
        routeHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView routeTitle = text("3. Liên kết chuyên gia", 17, Color.rgb(15, 23, 42), Typeface.BOLD);
        routeHeader.addView(routeTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        routes.addView(routeHeader);
        TextView hint = text("Chọn nguồn Zalo rồi chọn nhóm Telegram theo tên. Không cần tự tìm Chat ID.", 13, Color.rgb(100, 116, 139), Typeface.NORMAL);
        hint.setPadding(0, dp(8), 0, dp(10));
        routes.addView(hint);
        Button add = primaryButton("+ THÊM TỪ ZALO GẦN ĐÂY");
        add.setOnClickListener(v -> chooseRecentSource());
        routes.addView(add, lp(-1, dp(48), 0, 0, 0, 12));
        routesBox = new LinearLayout(this);
        routesBox.setOrientation(LinearLayout.VERTICAL);
        routes.addView(routesBox);
        root.addView(routes, lp(-1, -2, 0, 0, 0, 12));

        LinearLayout stats = card();
        stats.addView(text("Theo dõi", 17, Color.rgb(15, 23, 42), Typeface.BOLD));
        statsText = text("", 14, Color.rgb(51, 65, 85), Typeface.NORMAL);
        statsText.setPadding(0, dp(8), 0, 0);
        stats.addView(statsText);
        root.addView(stats);

        TextView foot = text("Bridge chỉ forward nguồn Zalo đã được liên kết. Nội dung không được AI sửa đổi. Bot Token được mã hóa bằng Android Keystore trên thiết bị.", 12, Color.rgb(100, 116, 139), Typeface.NORMAL);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(dp(8), dp(16), dp(8), 0);
        root.addView(foot);
        return scroll;
    }

    private void refreshStatus() {
        boolean granted = hasListenerAccess();
        boolean connected = BridgePrefs.listenerConnected(this);
        listenerStatus.setText(granted
                ? (connected ? "● Đã cấp quyền — Listener đang kết nối" : "● Đã cấp quyền — đang chờ Listener")
                : "● Chưa cấp Notification Access");
        listenerStatus.setTextColor(granted ? Color.rgb(22, 101, 52) : Color.rgb(185, 28, 28));

        boolean bot = SecureStore.hasBotToken(this);
        boolean tested = BridgePrefs.prefs(this).getBoolean("telegram_test_ok", false);
        telegramStatus.setText(bot ? (tested ? "● Bot Token đã lưu — kết nối kiểm tra OK" : "● Bot Token đã lưu — chưa kiểm tra") : "● Chưa cấu hình Bot Token");
        telegramStatus.setTextColor(bot ? Color.rgb(22, 101, 52) : Color.rgb(180, 83, 9));
        String username = BridgePrefs.prefs(this).getString("telegram_bot_username", "");
        botIdentity.setText(username == null || username.isEmpty()
                ? ""
                : "Bot đang dùng: @" + username + "\nĐể nhận diện group private, thêm bot vào group rồi gửi /bridge@" + username);

        boolean enabled = BridgePrefs.enabled(this);
        bridgeStatus.setText(enabled ? "● ĐANG CHUYỂN TIẾP" : "● ĐÃ TẠM DỪNG");
        bridgeStatus.setTextColor(enabled ? Color.rgb(22, 101, 52) : Color.rgb(180, 83, 9));

        long seen = BridgePrefs.prefs(this).getLong("zalo_seen", 0L);
        long forwarded = BridgePrefs.prefs(this).getLong("forwarded", 0L);
        long failed = BridgePrefs.prefs(this).getLong("failed", 0L);
        long lastForward = BridgePrefs.prefs(this).getLong("last_forward_ms", 0L);
        String lastError = BridgePrefs.prefs(this).getString("last_error", "");
        StringBuilder s = new StringBuilder();
        s.append("Zalo đã thấy: ").append(seen)
                .append("\nĐã forward: ").append(forwarded)
                .append("\nĐang chờ gửi: ").append(QueueStore.size(this))
                .append("\nLỗi/retry: ").append(failed);
        if (lastForward > 0) s.append("\nForward gần nhất: ").append(time(lastForward));
        if (lastError != null && !lastError.isEmpty()) s.append("\nLỗi gần nhất: ").append(lastError);
        statsText.setText(s.toString());
        renderRoutes();
    }

    private void renderRoutes() {
        if (routesBox == null) return;
        routesBox.removeAllViews();
        List<Route> routes = RouteStore.load(this);
        if (routes.isEmpty()) {
            routesBox.addView(text("Chưa có liên kết nào.", 14, Color.rgb(100, 116, 139), Typeface.NORMAL));
            return;
        }
        for (Route r : routes) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(12), dp(12), dp(12), dp(12));
            row.setBackground(roundRect(Color.rgb(248, 250, 252), 12));
            TextView name = text((r.enabled ? "● " : "○ ") + r.label, 15, r.enabled ? Color.rgb(22, 101, 52) : Color.rgb(71, 85, 105), Typeface.BOLD);
            row.addView(name);
            String target = r.telegramChat == null || r.telegramChat.trim().isEmpty() ? "Chưa chọn Telegram đích" : r.telegramChat;
            TextView meta = text("Zalo ID: " + r.notificationId + "\nTelegram: " + target, 12, Color.rgb(100, 116, 139), Typeface.NORMAL);
            meta.setPadding(0, dp(4), 0, dp(8));
            row.addView(meta);
            Button edit = secondaryButton("CHỈNH SỬA / KIỂM TRA");
            edit.setOnClickListener(v -> editRoute(r));
            row.addView(edit, lp(-1, dp(42), 0, 0, 0, 0));
            routesBox.addView(row, lp(-1, -2, 0, 0, 0, 8));
        }
    }

    private void chooseRecentSource() {
        List<RecentSourceStore.Source> sources = RecentSourceStore.load(this);
        if (sources.isEmpty()) {
            Toast.makeText(this, "Chưa thấy nguồn Zalo mới. Hãy chờ nhóm/chuyên gia gửi một notification rồi thử lại.", Toast.LENGTH_LONG).show();
            return;
        }
        String[] labels = new String[sources.size()];
        for (int i = 0; i < sources.size(); i++) {
            RecentSourceStore.Source s = sources.get(i);
            String title = s.title == null || s.title.isEmpty() ? "Nguồn Zalo" : s.title;
            String preview = s.preview == null ? "" : s.preview;
            labels[i] = title + "\n" + (preview.length() > 60 ? preview.substring(0, 60) + "…" : preview) + "\nID " + s.notificationId;
        }
        new AlertDialog.Builder(this)
                .setTitle("Chọn nguồn Zalo")
                .setItems(labels, (d, which) -> linkSource(sources.get(which)))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void linkSource(RecentSourceStore.Source source) {
        Route existing = null;
        for (Route r : RouteStore.load(this)) {
            if (r.notificationId == source.notificationId && (r.telegramChat == null || r.telegramChat.trim().isEmpty())) { existing = r; break; }
        }
        Route route = existing == null ? new Route() : existing;
        route.notificationId = source.notificationId;
        route.keyHash = source.keyHash;
        if (route.label == null || route.label.startsWith("Chuyên gia")) route.label = source.title == null || source.title.isEmpty() ? "Chuyên gia Zalo" : source.title;
        editRoute(route);
    }

    private void editRoute(Route route) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(4), dp(20), 0);

        EditText label = field(route.label, "Tên chuyên gia / nhóm Zalo");
        EditText chat = field(route.telegramChat, "Telegram Chat ID");
        chat.setFocusable(false);
        chat.setClickable(true);
        CheckBox enabled = new CheckBox(this);
        enabled.setText("Bật auto forward");
        enabled.setChecked(route.enabled);
        form.addView(label, lp(-1, dp(52), 0, 8, 0, 8));
        form.addView(chat, lp(-1, dp(52), 0, 0, 0, 6));
        Button chooseTg = secondaryButton("CHỌN NHÓM TELEGRAM");
        chooseTg.setOnClickListener(v -> scanTelegramChats(chat));
        form.addView(chooseTg, lp(-1, dp(44), 0, 0, 0, 8));
        form.addView(enabled);
        TextView source = text("Nguồn Zalo ID: " + route.notificationId, 12, Color.rgb(100, 116, 139), Typeface.NORMAL);
        source.setPadding(0, dp(6), 0, 0);
        form.addView(source);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Liên kết Zalo → Telegram")
                .setView(form)
                .setNegativeButton("Hủy", null)
                .setNeutralButton("Xóa", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.setOnShowListener(x -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String l = label.getText().toString().trim();
                String c = chat.getText().toString().trim();
                if (l.isEmpty() || c.isEmpty()) {
                    Toast.makeText(this, "Cần chọn tên và nhóm Telegram đích.", Toast.LENGTH_LONG).show();
                    return;
                }
                route.label = l;
                route.telegramChat = c;
                route.enabled = enabled.isChecked();
                RouteStore.upsert(this, route);
                DedupStore.resetRoute(this, route.id);
                dialog.dismiss();
                refreshStatus();
                testRoute(route);
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                RouteStore.delete(this, route.id);
                dialog.dismiss();
                refreshStatus();
            });
        });
        dialog.show();
    }

    private void testTelegram() {
        String typed = tokenInput.getText().toString().trim();
        if (!typed.isEmpty()) {
            if (!SecureStore.saveBotToken(this, typed)) {
                Toast.makeText(this, "Không lưu được Bot Token vào Android Keystore.", Toast.LENGTH_LONG).show();
                return;
            }
            tokenInput.setText("");
            tokenInput.setHint("Bot Token đã lưu — nhập mới để thay");
        }
        String token = SecureStore.getBotToken(this);
        if (token.isEmpty()) {
            Toast.makeText(this, "Hãy nhập Bot Token trước.", Toast.LENGTH_LONG).show();
            return;
        }
        telegramStatus.setText("● Đang kiểm tra Telegram...");
        new Thread(() -> {
            TelegramDiscovery.BotProfile p = TelegramDiscovery.profile(token);
            BridgePrefs.prefs(this).edit()
                    .putBoolean("telegram_test_ok", p.ok)
                    .putString("telegram_bot_username", p.ok ? p.username : "")
                    .putString("last_error", p.ok ? "" : p.error)
                    .apply();
            runOnUiThread(() -> {
                String msg = p.ok ? "Telegram Bot kết nối OK: @" + p.username : "Telegram lỗi: " + p.error;
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                refreshStatus();
                if (p.ok) TelegramDispatcher.kick(this);
            });
        }).start();
    }

    private void scanTelegramChats(EditText targetField) {
        String token = SecureStore.getBotToken(this);
        if (token.isEmpty()) {
            Toast.makeText(this, "Hãy lưu Bot Token trước.", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "Đang quét group/channel Telegram...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            TelegramDiscovery.ScanResult r = TelegramDiscovery.scanChats(token);
            if (r.bot != null && r.bot.ok) {
                BridgePrefs.prefs(this).edit().putString("telegram_bot_username", r.bot.username).apply();
            }
            if (r.ok && !r.chats.isEmpty()) TelegramChatStore.merge(this, r.chats);
            List<TelegramDiscovery.ChatCandidate> cached = TelegramChatStore.load(this);
            runOnUiThread(() -> {
                refreshStatus();
                if (!r.ok) {
                    new AlertDialog.Builder(this)
                            .setTitle("Không quét được Telegram")
                            .setMessage(r.message)
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }
                if (cached.isEmpty()) {
                    String username = r.bot == null ? "" : r.bot.username;
                    String command = username == null || username.isEmpty() ? "/bridge" : "/bridge@" + username;
                    new AlertDialog.Builder(this)
                            .setTitle("Chưa thấy nhóm Telegram")
                            .setMessage("1. Thêm bot vào group/channel cần nhận tín hiệu.\n2. Trong group gửi đúng lệnh:\n\n" + command + "\n\n3. Quay lại app và bấm QUÉT NHÓM TELEGRAM lần nữa.\n\nBot không cần trả lời lệnh này; app chỉ dùng update để nhận diện group private.")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }
                showTelegramChoices(cached, targetField);
            });
        }).start();
    }

    private void showTelegramChoices(List<TelegramDiscovery.ChatCandidate> chats, EditText targetField) {
        String[] labels = new String[chats.size()];
        for (int i = 0; i < chats.size(); i++) {
            TelegramDiscovery.ChatCandidate c = chats.get(i);
            labels[i] = c.displayName() + "\n" + c.type + " • " + c.id;
        }
        new AlertDialog.Builder(this)
                .setTitle(targetField == null ? "Nhóm Telegram đã phát hiện" : "Chọn Telegram đích")
                .setItems(labels, (d, which) -> {
                    TelegramDiscovery.ChatCandidate c = chats.get(which);
                    if (targetField != null) {
                        targetField.setText(c.target());
                        Toast.makeText(this, "Đã chọn “" + c.displayName() + "”", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Đã phát hiện “" + c.displayName() + "” • ID " + c.id, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void testRoute(Route route) {
        String token = SecureStore.getBotToken(this);
        if (token.isEmpty()) return;
        new Thread(() -> {
            TelegramApi.Result r = TelegramApi.testChat(token, route.telegramChat);
            runOnUiThread(() -> Toast.makeText(this,
                    r.ok ? "Telegram đích “" + route.label + "” hợp lệ." : "Không truy cập được Telegram đích: " + r.message,
                    Toast.LENGTH_LONG).show());
        }).start();
    }

    private boolean hasListenerAccess() {
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (TextUtils.isEmpty(flat)) return false;
        String mine = new ComponentName(this, ZaloNotificationListener.class).flattenToString();
        for (String item : flat.split(":")) if (mine.equals(item)) return true;
        return false;
    }

    private void openNotificationAccess() {
        try { startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")); }
        catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
    }

    private EditText field(String value, String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value == null ? "" : value);
        e.setSingleLine(true);
        e.setTextSize(14);
        e.setPadding(dp(12), dp(8), dp(12), dp(8));
        e.setBackground(roundRect(Color.rgb(248, 250, 252), 12));
        return e;
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(16), dp(16), dp(16));
        l.setBackground(roundRect(Color.WHITE, 16));
        l.setElevation(dp(1));
        return l;
    }

    private TextView text(String s, int sp, int color, int style) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans", style));
        return t;
    }

    private Button primaryButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(12);
        b.setTextColor(Color.WHITE);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false);
        b.setBackground(roundRect(Color.rgb(37, 99, 235), 12));
        return b;
    }

    private Button secondaryButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(12);
        b.setTextColor(Color.rgb(30, 64, 175));
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false);
        b.setBackground(roundRect(Color.rgb(239, 246, 255), 12));
        return b;
    }

    private GradientDrawable roundRect(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private String time(long ms) {
        return new SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(new Date(ms));
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
