package com.pipsniper.zaloprobe;

import android.content.Context;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class RouteStore {
    private static final String KEY = "routes_json_v1";

    private RouteStore() {}

    public static synchronized List<Route> load(Context c) {
        List<Route> out = new ArrayList<>();
        String raw = BridgePrefs.prefs(c).getString(KEY, "[]");
        try {
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) out.add(Route.fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) {}
        Collections.sort(out, Comparator.comparingLong(r -> r.createdAt));
        return out;
    }

    public static synchronized void save(Context c, List<Route> routes) {
        JSONArray a = new JSONArray();
        if (routes != null) {
            for (Route r : routes) {
                try { a.put(r.toJson()); } catch (Exception ignored) {}
            }
        }
        BridgePrefs.prefs(c).edit().putString(KEY, a.toString()).apply();
    }

    public static synchronized void upsert(Context c, Route route) {
        List<Route> routes = load(c);
        boolean replaced = false;
        for (int i = 0; i < routes.size(); i++) {
            Route existing = routes.get(i);
            if (existing.id.equals(route.id) || existing.notificationId == route.notificationId) {
                routes.set(i, route);
                replaced = true;
                break;
            }
        }
        if (!replaced) routes.add(route);
        save(c, routes);
    }

    public static synchronized void delete(Context c, String id) {
        List<Route> routes = load(c);
        routes.removeIf(r -> r.id.equals(id));
        save(c, routes);
    }

    public static synchronized Route find(Context c, int notificationId) {
        for (Route r : load(c)) if (r.notificationId == notificationId) return r;
        return null;
    }

    public static synchronized void ensureSeedRoutes(Context c) {
        List<Route> routes = load(c);
        boolean changed = false;
        if (routes.stream().noneMatch(r -> r.notificationId == -1886333271)) {
            Route r = new Route();
            r.label = "Thắng Nguyễn";
            r.notificationId = -1886333271;
            r.enabled = false;
            routes.add(r);
            changed = true;
        }
        if (routes.stream().noneMatch(r -> r.notificationId == -82325455)) {
            Route r = new Route();
            r.label = "Tú Phạm Trading";
            r.notificationId = -82325455;
            r.enabled = false;
            routes.add(r);
            changed = true;
        }
        if (changed) save(c, routes);
    }
}
