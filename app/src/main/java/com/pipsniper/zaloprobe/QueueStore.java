package com.pipsniper.zaloprobe;

import android.content.Context;
import android.util.AtomicFile;

import org.json.JSONArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class QueueStore {
    private static final int MAX_ITEMS = 5000;
    private static final String FILE_NAME = "bridge_forward_queue_v1.json";

    private QueueStore() {}

    private static AtomicFile atomic(Context c) {
        return new AtomicFile(new File(c.getFilesDir(), FILE_NAME));
    }

    public static synchronized List<QueueItem> load(Context c) {
        List<QueueItem> out = new ArrayList<>();
        AtomicFile af = atomic(c);
        if (!af.getBaseFile().exists()) return out;
        try (FileInputStream in = af.openRead()) {
            byte[] bytes = new byte[(int) Math.min(8_000_000L, af.getBaseFile().length())];
            int n = in.read(bytes);
            if (n <= 0) return out;
            JSONArray a = new JSONArray(new String(bytes, 0, n, StandardCharsets.UTF_8));
            for (int i = 0; i < a.length(); i++) out.add(QueueItem.fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) {}
        out.sort(Comparator.comparingLong(i -> i.createdAt));
        return out;
    }

    private static synchronized void save(Context c, List<QueueItem> list) {
        JSONArray a = new JSONArray();
        for (QueueItem i : list) {
            try { a.put(i.toJson()); } catch (Exception ignored) {}
        }
        AtomicFile af = atomic(c);
        FileOutputStream out = null;
        try {
            out = af.startWrite();
            out.write(a.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();
            af.finishWrite(out);
        } catch (Exception e) {
            if (out != null) af.failWrite(out);
        }
    }

    public static synchronized boolean enqueue(Context c, QueueItem item) {
        if (item == null || item.chatId == null || item.chatId.trim().isEmpty()) return false;
        List<QueueItem> list = load(c);
        for (QueueItem existing : list) {
            if (!item.contentHash.isEmpty()
                    && item.contentHash.equals(existing.contentHash)
                    && item.chatId.equals(existing.chatId)) return true;
        }
        if (list.size() >= MAX_ITEMS) {
            BridgePrefs.prefs(c).edit().putBoolean("queue_overflow", true).apply();
            return false;
        }
        list.add(item);
        save(c, list);
        return true;
    }

    public static synchronized void update(Context c, QueueItem item) {
        List<QueueItem> list = load(c);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(item.id)) {
                list.set(i, item);
                save(c, list);
                return;
            }
        }
    }

    public static synchronized void remove(Context c, String id) {
        List<QueueItem> list = load(c);
        QueueItem removed = null;
        for (QueueItem i : list) if (i.id.equals(id)) { removed = i; break; }
        list.removeIf(i -> i.id.equals(id));
        save(c, list);
        if (removed != null && removed.hasMedia()) {
            try { new File(removed.mediaPath).delete(); } catch (Exception ignored) {}
        }
    }

    public static synchronized int size(Context c) {
        return load(c).size();
    }

    public static synchronized QueueItem firstDue(Context c, long now) {
        for (QueueItem i : load(c)) if (i.nextAttemptAt <= now) return i;
        return null;
    }

    public static synchronized long nextDueAt(Context c) {
        long next = Long.MAX_VALUE;
        for (QueueItem i : load(c)) next = Math.min(next, i.nextAttemptAt);
        return next == Long.MAX_VALUE ? 0L : next;
    }

    public static String saveMedia(Context c, String id, byte[] bytes) throws Exception {
        File dir = new File(c.getFilesDir(), "bridge_queue_media");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create media queue directory");
        File f = new File(dir, id + ".png");
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(bytes);
            out.flush();
        }
        return f.getAbsolutePath();
    }
}
