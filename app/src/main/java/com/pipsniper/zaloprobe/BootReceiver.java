package com.pipsniper.zaloprobe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        RouteStore.ensureSeedRoutes(context);
        TelegramDispatcher.kick(context);
    }
}
