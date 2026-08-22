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
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(248, 250, 253);
    private static final int TEXT = Color.rgb(18, 25, 38);
    private static final int MUTED = Color.rgb(102, 112, 133);
    private static final int BLUE = Color.rgb(10, 102, 255);
    private static final int BLUE_SOFT = Color.rgb(236, 244, 255);
    private static final int GREEN = Color.rgb(24, 153, 82);
    private static final int GREEN_SOFT = Color.rgb(235, 249, 241);
    private static final int RED = Color.rgb(220, 56, 52);
    private static final int AMBER = Color.rgb(211, 132, 24);

    private TextView listenerStatus;
    private TextView telegramStatus;
    private TextView botIdentity;
    private TextView bridgeStatus;
    private TextView statsSeen;
    private TextView statsForwarded;
    private TextView statsQueue;
    private TextView statsFailed;
    private TextView statsLast;
    private LinearLayout routesBox;
    private EditText tokenInput;
    private EditText searchInput;
    private String routeFilter = "all";

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
        RouteStore.ensureSeedRoutes(this);
        setContentView(buildUi());
        TelegramDispatcher.kick(this);
        TelegramBotRuntime.start(this);
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
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        root.addView(buildHeader());
        root.addView(buildSearch(), lp(-1, dp(54), 0, 14, 0, 0));
        root.addView(buildFilters(), lp(-1, dp(48), 0, 10, 0, 0));
        root.addView(buildBridgeStrip(), lp(-1, -2, 0, 14, 0, 0));
        root.addView(buildExpertsCard(), lp(-1, -2, 0, 12, 0, 0));
        root.addView(buildStatsCard(), lp(-1, -2, 0, 12, 0, 0));
        root.addView(buildQuickActions(), lp(-1, -2, 0, 12, 0, 0));
        root.addView(buildConnectionCard(), lp(-1, -2, 0, 12, 0, 0));

        TextView foot = text("Chỉ forward nguồn Zalo đã liên kết • Tín hiệu được giữ nguyên • Bot Token lưu trong Android Keystore", 11, MUTED, Typeface.NORMAL);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(dp(8), dp(12), dp(8), dp(4));
        root.addView(foot);

        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        shell.addView(buildBottomNav(), new LinearLayout.LayoutParams(-1, dp(68)));
        return shell;
    }

    private View buildHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_launcher_butterfly);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        row.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(12), 0, 0, 0);
        titles.addView(text("PipSniper Zalo Bridge", 22, TEXT, Typeface.BOLD));
        titles.addView(text("Zalo → Telegram • Smart Bot", 12, MUTED, Typeface.NORMAL));
        row.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));

        Button more = iconButton("⋮");
        more.setOnClickListener(v -> showSystemMenu());
        row.addView(more, new LinearLayout.LayoutParams(dp(44), dp(44)));
        return row;
    }

    private View buildSearch() {
        searchInput = new EditText(this);
        searchInput.setHint("Tìm chuyên gia, nhóm Telegram...");
        searchInput.setSingleLine(true);
        searchInput.setTextSize(14);
        searchInput.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_search, 0, 0, 0);
        searchInput.setCompoundDrawablePadding(dp(10));
        searchInput.setPadding(dp(14), 0, dp(14), 0);
        searchInput.setBackground(roundRect(Color.rgb(241, 244, 249), 24));
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderRoutes(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        return searchInput;
    }

    private View buildFilters() {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(filterChip("Tất cả", "all", true));
        row.addView(filterChip("● Đang chạy", "running", false), lp(-2, dp(42), 8, 0, 0, 0));
        row.addView(filterChip("Chuyên gia", "experts", false), lp(-2, dp(42), 8, 0, 0, 0));
        row.addView(filterChip("Theo dõi", "stats", false), lp(-2, dp(42), 8, 0, 0, 0));
        hsv.addView(row);
        return hsv;
    }

    private Button filterChip(String label, String filter, boolean selected) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setPadding(dp(14), 0, dp(14), 0);
        b.setTextColor(selected ? Color.WHITE : Color.rgb(75, 85, 99));
        b.setBackground(roundRect(selected ? BLUE : Color.rgb(240, 242, 247), 22));
        b.setOnClickListener(v -> {
            routeFilter = filter;
            if ("stats".equals(filter)) {
                Toast.makeText(this, "Theo dõi hệ thống hiển thị ngay bên dưới danh sách chuyên gia.", Toast.LENGTH_SHORT).show();
                routeFilter = "all";
            }
            renderRoutes();
        });
        return b;
    }

    private View buildBridgeStrip() {
        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER_VERTICAL);
        strip.setPadding(dp(14), dp(10), dp(14), dp(10));
        strip.setBackground(roundRect(Color.WHITE, 16));
        strip.setElevation(dp(1));
        bridgeStatus = text("", 14, GREEN, Typeface.BOLD);
        strip.addView(bridgeStatus, new LinearLayout.LayoutParams(0, -2, 1f));
        Button toggle = secondaryButton("BẬT / TẮT");
        toggle.setOnClickListener(v -> {
            BridgePrefs.setEnabled(this, !BridgePrefs.enabled(this));
            if (BridgePrefs.enabled(this)) TelegramDispatcher.kick(this);
            refreshStatus();
        });
        strip.addView(toggle, new LinearLayout.LayoutParams(dp(98), dp(40)));
        return strip;
    }

    private View buildExpertsCard() {
        LinearLayout card = card();
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text("Chuyên gia liên kết", 18, TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1f));
        Button add = outlineButton("＋ Thêm nguồn");
        add.setOnClickListener(v -> chooseRecentSource());
        head.addView(add, new LinearLayout.LayoutParams(dp(126), dp(42)));
        card.addView(head);
        TextView hint = text("Chọn nguồn Zalo mới rồi gán tới group/channel Telegram theo tên.", 12, MUTED, Typeface.NORMAL);
        hint.setPadding(0, dp(6), 0, dp(8));
        card.addView(hint);
        routesBox = new LinearLayout(this);
        routesBox.setOrientation(LinearLayout.VERTICAL);
        card.addView(routesBox);
        return card;
    }

    private View buildStatsCard() {
        LinearLayout card = card();
        card.addView(text("Theo dõi hệ thống", 18, TEXT, Typeface.BOLD));
        TextView sub = text("Số liệu cập nhật tự động", 12, MUTED, Typeface.NORMAL);
        sub.setPadding(0, dp(4), 0, dp(10));
        card.addView(sub);
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        statsSeen = metric(row, "👁", "0", "Zalo đã thấy", BLUE);
        statsForwarded = metric(row, "➤", "0", "Đã forward", GREEN);
        statsQueue = metric(row, "◷", "0", "Đang chờ", AMBER);
        statsFailed = metric(row, "⚠", "0", "Lỗi / retry", RED);
        statsLast = metric(row, "▣", "--", "Forward gần nhất", Color.rgb(124, 58, 237));
        hsv.addView(row);
        card.addView(hsv);
        return card;
    }

    private TextView metric(LinearLayout parent, String icon, String value, String label, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(8), dp(8), dp(8), dp(8));
        TextView i = text(icon, 20, color, Typeface.BOLD);
        i.setGravity(Gravity.CENTER);
        box.addView(i);
        TextView v = text(value, 18, TEXT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        box.addView(v);
        TextView l = text(label, 10, MUTED, Typeface.NORMAL);
        l.setGravity(Gravity.CENTER);
        box.addView(l);
        parent.addView(box, new LinearLayout.LayoutParams(dp(112), dp(94)));
        return v;
    }

    private View buildQuickActions() {
        LinearLayout card = card();
        card.addView(text("Thao tác nhanh", 18, TEXT, Typeface.BOLD));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, 0);
        Button add = quickButton("＋\nThêm nguồn", BLUE_SOFT, BLUE);
        add.setOnClickListener(v -> chooseRecentSource());
        row.addView(add, new LinearLayout.LayoutParams(0, dp(74), 1f));
        Button scan = quickButton("➤\nQuét Telegram", GREEN_SOFT, GREEN);
        scan.setOnClickListener(v -> scanTelegramChats(null));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(74), 1f);
        p.setMargins(dp(8), 0, 0, 0);
        row.addView(scan, p);
        Button test = quickButton("⌕\nKiểm tra", Color.rgb(247, 240, 255), Color.rgb(124, 58, 237));
        test.setOnClickListener(v -> showConnectionDialog());
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, dp(74), 1f);
        p2.setMargins(dp(8), 0, 0, 0);
        row.addView(test, p2);
        card.addView(row);
        return card;
    }

    private View buildConnectionCard() {
        LinearLayout card = card();
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(text("Kết nối", 18, TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1f));
        Button config = outlineButton("Cấu hình");
        config.setOnClickListener(v -> showConnectionDialog());
        titleRow.addView(config, new LinearLayout.LayoutParams(dp(96), dp(40)));
        card.addView(titleRow);
        listenerStatus = text("Đang kiểm tra Zalo...", 13, MUTED, Typeface.NORMAL);
        listenerStatus.setPadding(0, dp(10), 0, dp(4));
        card.addView(listenerStatus);
        telegramStatus = text("Đang kiểm tra Telegram...", 13, MUTED, Typeface.NORMAL);
        card.addView(telegramStatus);
        botIdentity = text("", 12, MUTED, Typeface.NORMAL);
        botIdentity.setPadding(0, dp(4), 0, 0);
        card.addView(botIdentity);
        return card;
    }

    private View buildBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(6), dp(4), dp(6), dp(4));
        nav.setBackgroundColor(Color.WHITE);
        nav.setElevation(dp(8));
        nav.addView(navButton("⌂", "Trang chủ", true, v -> {}), new LinearLayout.LayoutParams(0, -1, 1f));
        nav.addView(navButton("⇄", "Kết nối", false, v -> showConnectionDialog()), new LinearLayout.LayoutParams(0, -1, 1f));
        nav.addView(navButton("➤", "Telegram", false, v -> scanTelegramChats(null)), new LinearLayout.LayoutParams(0, -1, 1f));
        nav.addView(navButton("☰", "Theo dõi", false, v -> showStatsDialog()), new LinearLayout.LayoutParams(0, -1, 1f));
        nav.addView(navButton("BOT", "Smart Bot", false, v -> showBotHelp()), new LinearLayout.LayoutParams(0, -1, 1f));
        return nav;
    }

    private Button navButton(String icon, String label, boolean active, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(icon + "\n" + label);
        b.setTextSize(10);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setTextColor(active ? BLUE : Color.rgb(117, 126, 140));
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setOnClickListener(listener);
        return b;
    }

    private void refreshStatus() {
        boolean granted = hasListenerAccess();
        boolean connected = BridgePrefs.listenerConnected(this);
        if (listenerStatus != null) {
            listenerStatus.setText(granted ? (connected ? "● Zalo Listener đang kết nối" : "● Đã cấp quyền Zalo — đang chờ Listener") : "● Chưa cấp Notification Access cho Zalo");
            listenerStatus.setTextColor(granted ? GREEN : RED);
        }
        boolean bot = SecureStore.hasBotToken(this);
        boolean tested = BridgePrefs.prefs(this).getBoolean("telegram_test_ok", false);
        if (telegramStatus != null) {
            telegramStatus.setText(bot ? (tested ? "● Telegram Bot kết nối OK" : "● Bot Token đã lưu — chưa kiểm tra") : "● Chưa cấu hình Telegram Bot Token");
            telegramStatus.setTextColor(bot && tested ? GREEN : AMBER);
        }
        String username = BridgePrefs.prefs(this).getString("telegram_bot_username", "");
        if (botIdentity != null) botIdentity.setText(username == null || username.isEmpty() ? "" : "Bot: @" + username + " • /status /health /routes /stats");
        boolean enabled = BridgePrefs.enabled(this);
        if (bridgeStatus != null) {
            bridgeStatus.setText(enabled ? "● ĐANG CHUYỂN TIẾP" : "● ĐÃ TẠM DỪNG");
            bridgeStatus.setTextColor(enabled ? GREEN : AMBER);
        }
        long seen = BridgePrefs.prefs(this).getLong("zalo_seen", 0L);
        long forwarded = BridgePrefs.prefs(this).getLong("forwarded", 0L);
        long failed = BridgePrefs.prefs(this).getLong("failed", 0L);
        long lastForward = BridgePrefs.prefs(this).getLong("last_forward_ms", 0L);
        if (statsSeen != null) statsSeen.setText(String.valueOf(seen));
        if (statsForwarded != null) statsForwarded.setText(String.valueOf(forwarded));
        if (statsQueue != null) statsQueue.setText(String.valueOf(QueueStore.size(this)));
        if (statsFailed != null) statsFailed.setText(String.valueOf(failed));
        if (statsLast != null) statsLast.setText(lastForward > 0 ? new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date(lastForward)) : "--");
        renderRoutes();
    }

    private void renderRoutes() {
        if (routesBox == null) return;
        routesBox.removeAllViews();
        List<Route> routes = RouteStore.load(this);
        String q = searchInput == null ? "" : searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        int shown = 0;
        for (Route r : routes) {
            if ("running".equals(routeFilter) && !r.ready()) continue;
            String hay = ((r.label == null ? "" : r.label) + " " + (r.telegramChat == null ? "" : r.telegramChat)).toLowerCase(Locale.ROOT);
            if (!q.isEmpty() && !hay.contains(q)) continue;
            routesBox.addView(routeRow(r), lp(-1, -2, 0, shown == 0 ? 2 : 7, 0, 0));
            shown++;
        }
        if (shown == 0) {
            TextView empty = text(q.isEmpty() ? "Chưa có liên kết phù hợp." : "Không tìm thấy chuyên gia hoặc nhóm Telegram.", 13, MUTED, Typeface.NORMAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(18), 0, dp(18));
            routesBox.addView(empty);
        }
    }

    private View routeRow(Route r) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(6), dp(10), dp(4), dp(10));
        FrameLayout avatarWrap = new FrameLayout(this);
        TextView avatar = text(initials(r.label), 18, Color.WHITE, Typeface.BOLD);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(roundRect(avatarColor(r.label), 999));
        avatarWrap.addView(avatar, new FrameLayout.LayoutParams(dp(54), dp(54)));
        TextView dot = text("", 1, Color.WHITE, Typeface.NORMAL);
        dot.setBackground(roundRect(r.ready() ? GREEN : Color.LTGRAY, 999));
        FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(dp(15), dp(15), Gravity.RIGHT | Gravity.BOTTOM);
        dlp.setMargins(0, 0, dp(1), dp(1));
        avatarWrap.addView(dot, dlp);
        row.addView(avatarWrap, new LinearLayout.LayoutParams(dp(62), dp(58)));
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(8), 0, dp(6), 0);
        LinearLayout nameLine = new LinearLayout(this);
        nameLine.setOrientation(LinearLayout.HORIZONTAL);
        nameLine.setGravity(Gravity.CENTER_VERTICAL);
        nameLine.addView(text(r.label == null ? "Chuyên gia" : r.label, 15, TEXT, Typeface.BOLD));
        TextView badge = text(r.ready() ? " Đang chạy " : " Chưa cấu hình ", 10, r.ready() ? GREEN : AMBER, Typeface.BOLD);
        badge.setBackground(roundRect(r.ready() ? GREEN_SOFT : Color.rgb(255, 247, 230), 12));
        badge.setPadding(dp(6), dp(2), dp(6), dp(2));
        nameLine.addView(badge, lp(-2, -2, 8, 0, 0, 0));
        info.addView(nameLine);
        info.addView(text("Zalo → Telegram " + (r.ready() ? "đang chạy" : "chưa sẵn sàng"), 12, r.ready() ? GREEN : MUTED, Typeface.NORMAL));
        String tg = r.telegramChat == null || r.telegramChat.trim().isEmpty() ? "Chưa chọn Telegram" : r.telegramChat.trim();
        info.addView(text("Telegram: " + tg, 11, MUTED, Typeface.NORMAL));
        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));
        Button edit = secondaryButton("Kiểm tra");
        edit.setOnClickListener(v -> editRoute(r));
        row.addView(edit, new LinearLayout.LayoutParams(dp(86), dp(42)));
        return row;
    }

    private String initials(String label) {
        if (label == null || label.trim().isEmpty()) return "PS";
        String[] parts = label.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private int avatarColor(String label) {
        int h = label == null ? 0 : Math.abs(label.hashCode());
        int[][] palette = {{43, 120, 228}, {82, 94, 210}, {31, 157, 96}, {140, 75, 190}, {220, 94, 75}};
        int[] c = palette[h % palette.length];
        return Color.rgb(c[0], c[1], c[2]);
    }

    private void chooseRecentSource() {
        List<RecentSourceStore.Source> sources = RecentSourceStore.load(this);
        if (sources.isEmpty()) {
            Toast.makeText(this, "Chưa thấy nguồn Zalo mới. Hãy chờ nhóm/chuyên gia gửi notification rồi thử lại.", Toast.LENGTH_LONG).show();
            return;
        }
        String[] labels = new String[sources.size()];
        for (int i = 0; i < sources.size(); i++) {
            RecentSourceStore.Source s = sources.get(i);
            String title = s.title == null || s.title.isEmpty() ? "Nguồn Zalo" : s.title;
            String preview = s.preview == null ? "" : s.preview;
            labels[i] = title + "\n" + (preview.length() > 60 ? preview.substring(0, 60) + "…" : preview) + "\nID " + s.notificationId;
        }
        new AlertDialog.Builder(this).setTitle("Chọn nguồn Zalo").setItems(labels, (d, which) -> linkSource(sources.get(which))).setNegativeButton("Hủy", null).show();
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
        TextView source = text("Nguồn Zalo ID: " + route.notificationId, 12, MUTED, Typeface.NORMAL);
        source.setPadding(0, dp(6), 0, 0);
        form.addView(source);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Liên kết Zalo → Telegram").setView(form).setNegativeButton("Hủy", null).setNeutralButton("Xóa", null).setPositiveButton("Lưu", null).create();
        dialog.setOnShowListener(x -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String l = label.getText().toString().trim();
                String c = chat.getText().toString().trim();
                if (l.isEmpty() || c.isEmpty()) { Toast.makeText(this, "Cần chọn tên và nhóm Telegram đích.", Toast.LENGTH_LONG).show(); return; }
                route.label = l;
                route.telegramChat = c;
                route.enabled = enabled.isChecked();
                RouteStore.upsert(this, route);
                DedupStore.resetRoute(this, route.id);
                dialog.dismiss();
                refreshStatus();
                testRoute(route);
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> { RouteStore.delete(this, route.id); dialog.dismiss(); refreshStatus(); });
        });
        dialog.show();
    }

    private void showConnectionDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(6), dp(20), 0);
        TextView zalo = text(hasListenerAccess() ? "● Zalo Notification Access đã cấp" : "● Chưa cấp Zalo Notification Access", 13, hasListenerAccess() ? GREEN : RED, Typeface.BOLD);
        form.addView(zalo, lp(-1, -2, 0, 0, 0, 8));
        Button grant = secondaryButton("CẤP / KIỂM TRA QUYỀN ZALO");
        grant.setOnClickListener(v -> openNotificationAccess());
        form.addView(grant, lp(-1, dp(44), 0, 0, 0, 12));
        tokenInput = field("", SecureStore.hasBotToken(this) ? "Bot Token đã lưu — nhập mới để thay" : "Telegram Bot Token");
        tokenInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(tokenInput, lp(-1, dp(52), 0, 0, 0, 8));
        Button save = primaryButton("LƯU & KIỂM TRA TELEGRAM");
        save.setOnClickListener(v -> testTelegram());
        form.addView(save, lp(-1, dp(46), 0, 0, 0, 8));
        Button scan = secondaryButton("QUÉT NHÓM TELEGRAM");
        scan.setOnClickListener(v -> scanTelegramChats(null));
        form.addView(scan, lp(-1, dp(44), 0, 0, 0, 0));
        new AlertDialog.Builder(this).setTitle("Kết nối Zalo & Telegram").setView(form).setPositiveButton("Đóng", null).show();
    }

    private void testTelegram() {
        String typed = tokenInput == null ? "" : tokenInput.getText().toString().trim();
        if (!typed.isEmpty()) {
            if (!SecureStore.saveBotToken(this, typed)) { Toast.makeText(this, "Không lưu được Bot Token vào Android Keystore.", Toast.LENGTH_LONG).show(); return; }
            tokenInput.setText("");
            tokenInput.setHint("Bot Token đã lưu — nhập mới để thay");
        }
        String token = SecureStore.getBotToken(this);
        if (token.isEmpty()) { Toast.makeText(this, "Hãy nhập Bot Token trước.", Toast.LENGTH_LONG).show(); return; }
        if (telegramStatus != null) telegramStatus.setText("● Đang kiểm tra Telegram...");
        new Thread(() -> {
            TelegramDiscovery.BotProfile p = TelegramDiscovery.profile(token);
            BridgePrefs.prefs(this).edit().putBoolean("telegram_test_ok", p.ok).putString("telegram_bot_username", p.ok ? p.username : "").putString("last_error", p.ok ? "" : p.error).apply();
            if (p.ok) { TelegramBotClient.setDefaultCommands(token); TelegramBotRuntime.start(this); }
            runOnUiThread(() -> {
                Toast.makeText(this, p.ok ? "Telegram Bot kết nối OK: @" + p.username : "Telegram lỗi: " + p.error, Toast.LENGTH_LONG).show();
                refreshStatus();
                if (p.ok) TelegramDispatcher.kick(this);
            });
        }).start();
    }

    private void scanTelegramChats(EditText targetField) {
        String token = SecureStore.getBotToken(this);
        if (token.isEmpty()) { Toast.makeText(this, "Hãy lưu Bot Token trước.", Toast.LENGTH_LONG).show(); return; }
        Toast.makeText(this, "Đang quét group/channel Telegram...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            TelegramDiscovery.ScanResult r = TelegramDiscovery.scanChats(token);
            if (r.bot != null && r.bot.ok) BridgePrefs.prefs(this).edit().putString("telegram_bot_username", r.bot.username).apply();
            if (r.ok && !r.chats.isEmpty()) TelegramChatStore.merge(this, r.chats);
            List<TelegramDiscovery.ChatCandidate> cached = TelegramChatStore.load(this);
            runOnUiThread(() -> {
                refreshStatus();
                if (!r.ok) { new AlertDialog.Builder(this).setTitle("Không quét được Telegram").setMessage(r.message).setPositiveButton("OK", null).show(); return; }
                if (cached.isEmpty()) {
                    String username = r.bot == null ? "" : r.bot.username;
                    String command = username == null || username.isEmpty() ? "/bridge" : "/bridge@" + username;
                    new AlertDialog.Builder(this).setTitle("Chưa thấy nhóm Telegram").setMessage("1. Thêm bot vào group/channel cần nhận tín hiệu.\n2. Trong group gửi:\n\n" + command + "\n\n3. Quay lại app và quét lần nữa.").setPositiveButton("OK", null).show();
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
        new AlertDialog.Builder(this).setTitle(targetField == null ? "Nhóm Telegram đã phát hiện" : "Chọn Telegram đích").setItems(labels, (d, which) -> {
            TelegramDiscovery.ChatCandidate c = chats.get(which);
            if (targetField != null) { targetField.setText(c.target()); Toast.makeText(this, "Đã chọn “" + c.displayName() + "”", Toast.LENGTH_SHORT).show(); }
            else Toast.makeText(this, "Đã phát hiện “" + c.displayName() + "” • ID " + c.id, Toast.LENGTH_LONG).show();
        }).setNegativeButton("Đóng", null).show();
    }

    private void testRoute(Route route) {
        String token = SecureStore.getBotToken(this);
        if (token.isEmpty()) return;
        new Thread(() -> {
            TelegramApi.Result r = TelegramApi.testChat(token, route.telegramChat);
            runOnUiThread(() -> Toast.makeText(this, r.ok ? "Telegram đích “" + route.label + "” hợp lệ." : "Không truy cập được Telegram đích: " + r.message, Toast.LENGTH_LONG).show());
        }).start();
    }

    private void showStatsDialog() {
        long seen = BridgePrefs.prefs(this).getLong("zalo_seen", 0L);
        long forwarded = BridgePrefs.prefs(this).getLong("forwarded", 0L);
        long failed = BridgePrefs.prefs(this).getLong("failed", 0L);
        long last = BridgePrefs.prefs(this).getLong("last_forward_ms", 0L);
        String body = "Zalo đã thấy: " + seen + "\nĐã forward: " + forwarded + "\nĐang chờ gửi: " + QueueStore.size(this) + "\nLỗi/retry: " + failed + (last > 0 ? "\nForward gần nhất: " + time(last) : "");
        new AlertDialog.Builder(this).setTitle("Theo dõi hệ thống").setMessage(body).setPositiveButton("OK", null).show();
    }

    private void showBotHelp() {
        String username = BridgePrefs.prefs(this).getString("telegram_bot_username", "PipSniperZalo_bot");
        String body = "Bot: @" + username + "\n\n/status — trạng thái\n/health — kiểm tra hệ thống\n/routes — các liên kết\n/stats — thống kê\n/pause — tạm dừng\n/resume — bật lại\n/bridge — nhận diện group\n/whereami — Chat ID\n/test — kiểm tra bot";
        new AlertDialog.Builder(this).setTitle("PipSniper Smart Bot").setMessage(body).setPositiveButton("OK", null).show();
    }

    private void showSystemMenu() {
        String[] items = {"Cấu hình kết nối", "Quét nhóm Telegram", "Theo dõi hệ thống", "Smart Bot", BridgePrefs.enabled(this) ? "Tạm dừng Bridge" : "Bật Bridge"};
        new AlertDialog.Builder(this).setTitle("PipSniper").setItems(items, (d, which) -> {
            if (which == 0) showConnectionDialog();
            else if (which == 1) scanTelegramChats(null);
            else if (which == 2) showStatsDialog();
            else if (which == 3) showBotHelp();
            else { BridgePrefs.setEnabled(this, !BridgePrefs.enabled(this)); if (BridgePrefs.enabled(this)) TelegramDispatcher.kick(this); refreshStatus(); }
        }).show();
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
        e.setBackground(roundRect(Color.rgb(247, 249, 252), 14));
        return e;
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(16), dp(16), dp(16));
        l.setBackground(roundRect(Color.WHITE, 18));
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
        b.setBackground(roundRect(BLUE, 14));
        return b;
    }

    private Button secondaryButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(11);
        b.setTextColor(Color.rgb(32, 73, 165));
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false);
        b.setBackground(roundRect(BLUE_SOFT, 14));
        return b;
    }

    private Button outlineButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(11);
        b.setTextColor(BLUE);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false);
        GradientDrawable g = roundRect(Color.WHITE, 14);
        g.setStroke(dp(1), Color.rgb(166, 197, 255));
        b.setBackground(g);
        return b;
    }

    private Button quickButton(String s, int bg, int fg) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(11);
        b.setTextColor(fg);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setBackground(roundRect(bg, 14));
        return b;
    }

    private Button iconButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(24);
        b.setTextColor(TEXT);
        b.setAllCaps(false);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setGravity(Gravity.CENTER);
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

    private String time(long ms) { return new SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(new Date(ms)); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
