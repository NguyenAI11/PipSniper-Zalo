package com.pipsniper.zaloprobe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_EXPORT = 1001;
    private TextView accessStatus;
    private TextView captureStatus;
    private TextView countText;
    private TextView lastText;
    private Button captureButton;
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
        setTitle("PipSniper Zalo Probe");
        setContentView(buildUi());
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
        scroll.setBackgroundColor(Color.rgb(244, 246, 250));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("PipSniper Zalo Probe", 26, Color.rgb(15, 23, 42), Typeface.BOLD);
        root.addView(title);
        TextView subtitle = text("Đo metadata notification Zalo trên chính điện thoại của bạn", 15, Color.rgb(71, 85, 105), Typeface.NORMAL);
        subtitle.setPadding(0, dp(6), 0, dp(14));
        root.addView(subtitle);

        TextView privacy = text("OFFLINE  •  KHÔNG CÓ QUYỀN INTERNET", 12, Color.rgb(22, 101, 52), Typeface.BOLD);
        privacy.setGravity(Gravity.CENTER);
        privacy.setPadding(dp(12), dp(9), dp(12), dp(9));
        privacy.setBackground(roundRect(Color.rgb(220, 252, 231), 14));
        root.addView(privacy, lp(-1, -2, 0, 0, 0, 18));

        LinearLayout accessCard = card();
        accessCard.addView(text("1. Quyền Notification Access", 18, Color.rgb(15, 23, 42), Typeface.BOLD));
        accessStatus = text("Đang kiểm tra...", 14, Color.rgb(71, 85, 105), Typeface.NORMAL);
        accessStatus.setPadding(0, dp(8), 0, dp(12));
        accessCard.addView(accessStatus);
        Button accessButton = primaryButton("CẤP / KIỂM TRA QUYỀN");
        accessButton.setOnClickListener(v -> openNotificationAccess());
        accessCard.addView(accessButton);
        root.addView(accessCard, lp(-1, -2, 0, 0, 0, 14));

        LinearLayout captureCard = card();
        captureCard.addView(text("2. Thu thập tự động", 18, Color.rgb(15, 23, 42), Typeface.BOLD));
        captureStatus = text("", 14, Color.rgb(71, 85, 105), Typeface.NORMAL);
        captureStatus.setPadding(0, dp(8), 0, dp(4));
        captureCard.addView(captureStatus);
        countText = text("0 sự kiện", 28, Color.rgb(37, 99, 235), Typeface.BOLD);
        countText.setPadding(0, dp(6), 0, dp(2));
        captureCard.addView(countText);
        lastText = text("Chưa có notification Zalo", 13, Color.rgb(100, 116, 139), Typeface.NORMAL);
        lastText.setPadding(0, 0, 0, dp(12));
        captureCard.addView(lastText);
        captureButton = secondaryButton("TẠM DỪNG THU THẬP");
        captureButton.setOnClickListener(v -> toggleCapture());
        captureCard.addView(captureButton);
        root.addView(captureCard, lp(-1, -2, 0, 0, 0, 14));

        LinearLayout guideCard = card();
        guideCard.addView(text("Cách test", 18, Color.rgb(15, 23, 42), Typeface.BOLD));
        TextView guide = text(
                "• Cấp Notification Access một lần.\n" +
                "• Để Zalo hoạt động bình thường trong vài giờ hoặc một ngày.\n" +
                "• Nên có tin nhắn từ nhiều nhóm khác nhau.\n" +
                "• Không cần forward và không cần mở PipSniper trong lúc thu thập.\n" +
                "• Khi đủ dữ liệu, quay lại đây và bấm Xuất báo cáo.",
                14, Color.rgb(51, 65, 85), Typeface.NORMAL);
        guide.setLineSpacing(dp(3), 1f);
        guide.setPadding(0, dp(10), 0, 0);
        guideCard.addView(guide);
        root.addView(guideCard, lp(-1, -2, 0, 0, 0, 14));

        Button export = primaryButton("XUẤT BÁO CÁO");
        export.setOnClickListener(v -> createReportDocument());
        root.addView(export, lp(-1, dp(52), 0, 0, 0, 10));

        Button clear = secondaryButton("XÓA DỮ LIỆU ĐÃ THU THẬP");
        clear.setOnClickListener(v -> confirmClear());
        root.addView(clear, lp(-1, dp(48), 0, 0, 0, 16));

        TextView foot = text("Chỉ notification từ com.zing.zalo được ghi. Nội dung và identifier trong báo cáo được hash bằng salt cục bộ; app không có INTERNET permission.", 12, Color.rgb(100, 116, 139), Typeface.NORMAL);
        foot.setGravity(Gravity.CENTER);
        foot.setLineSpacing(dp(2), 1f);
        root.addView(foot);
        return scroll;
    }

    private void refreshStatus() {
        boolean granted = hasListenerAccess();
        if (accessStatus != null) {
            accessStatus.setText(granted ? "● Đã cấp quyền — sẵn sàng nhận notification Zalo" : "● Chưa cấp quyền — app chưa thể thu thập");
            accessStatus.setTextColor(granted ? Color.rgb(22, 101, 52) : Color.rgb(185, 28, 28));
        }
        boolean enabled = ProbeStore.isCaptureEnabled(this);
        long count = ProbeStore.prefs(this).getLong("event_count", 0L);
        long last = ProbeStore.prefs(this).getLong("last_capture_ms", 0L);
        boolean overflow = ProbeStore.prefs(this).getBoolean("overflow", false);
        captureStatus.setText(enabled ? "● Đang thu thập cục bộ" : "● Đã tạm dừng");
        captureStatus.setTextColor(enabled ? Color.rgb(22, 101, 52) : Color.rgb(180, 83, 9));
        countText.setText(String.format(Locale.getDefault(), "%,d sự kiện", count));
        if (overflow) countText.setText(countText.getText() + "  •  ĐÃ ĐẠT GIỚI HẠN");
        lastText.setText(last > 0 ? "Gần nhất: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date(last)) : "Chưa có notification Zalo");
        captureButton.setText(enabled ? "TẠM DỪNG THU THẬP" : "TIẾP TỤC THU THẬP");
    }

    private boolean hasListenerAccess() {
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (TextUtils.isEmpty(flat)) return false;
        String mine = new ComponentName(this, ZaloNotificationListener.class).flattenToString();
        for (String item : flat.split(":")) if (mine.equals(item)) return true;
        return false;
    }

    private void openNotificationAccess() {
        try {
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void toggleCapture() {
        ProbeStore.setCaptureEnabled(this, !ProbeStore.isCaptureEnabled(this));
        refreshStatus();
    }

    private void createReportDocument() {
        long count = ProbeStore.prefs(this).getLong("event_count", 0L);
        if (count == 0) {
            Toast.makeText(this, "Chưa có dữ liệu Zalo để xuất.", Toast.LENGTH_LONG).show();
            return;
        }
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "PipSniper_Zalo_Probe_Report_" + stamp + ".json");
        startActivityForResult(i, REQ_EXPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_EXPORT || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        new Thread(() -> {
            try {
                long n = ReportExporter.export(this, uri);
                runOnUiThread(() -> Toast.makeText(this, "Đã xuất " + n + " sự kiện. Hãy gửi file JSON này cho tôi.", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Xuất báo cáo thất bại: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa dữ liệu Probe?")
                .setMessage("Chỉ dữ liệu metadata Zalo do Probe thu thập sẽ bị xóa. Zalo không bị thay đổi.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (d, w) -> {
                    ProbeStore.clear(this);
                    refreshStatus();
                    Toast.makeText(this, "Đã xóa dữ liệu Probe.", Toast.LENGTH_SHORT).show();
                }).show();
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(18), dp(18), dp(18), dp(18));
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
        b.setTextSize(13);
        b.setTextColor(Color.WHITE);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false);
        b.setBackground(roundRect(Color.rgb(37, 99, 235), 14));
        b.setPadding(dp(14), 0, dp(14), 0);
        return b;
    }

    private Button secondaryButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(13);
        b.setTextColor(Color.rgb(30, 64, 175));
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false);
        b.setBackground(roundRect(Color.rgb(239, 246, 255), 14));
        b.setPadding(dp(14), 0, dp(14), 0);
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

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
