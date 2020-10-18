package com.application.monsterjourney;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class Restarter extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //Log.i("Broadcast Listened", "Service tried to stop");
        //Toast.makeText(context, "Monster Journey is Running in the background", Toast.LENGTH_SHORT).show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, ForeGroundService.class));
        } else {
            context.startService(new Intent(context, ForeGroundService.class));
        }
    }
}
