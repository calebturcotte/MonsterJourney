package com.application.monsterjourney;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;

public class HungerService extends BroadcastReceiver {
    /**
     * BroadcastReceiver for our alarm
     */

//    public HungerService() {
//        super("mainservice");
//    }
//
//    public HungerService(String name) {
//        super(name);
//    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
//    @Override
//    protected void onHandleIntent(Intent intent) {
//        AsyncTask.execute(()->{
//            AppDatabase db = AppDatabase.getInstance(this);
//            List<Monster> monsterList = db.journeyDao().getMonster();
//            for(Monster monster : monsterList){
//                monster.dayPassed();
//            }
//            db.journeyDao().updateMonster(monsterList);
//        });
//        Toast.makeText(this, "alarm", Toast.LENGTH_SHORT).show();
//        //showNotification();
//    }

    @Override
    public void onReceive(Context context, Intent intent) {
        AsyncTask.execute(()->{
            AppDatabase db = AppDatabase.getInstance(context);
            List<Monster> monsterList = db.journeyDao().getMonster();
            for(Monster monster : monsterList){
                monster.dayPassed();
            }
            db.journeyDao().updateMonster(monsterList);
        });
        Toast.makeText(context, "hello", Toast.LENGTH_SHORT).show();
        //Toast.makeText(context, "alarm", Toast.LENGTH_SHORT).show();
    }

//    private void showNotification() {
//
//        Uri soundUri = RingtoneManager
//                .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//        Notification notification = new NotificationCompat.Builder(this)
//                .setContentTitle("Alarm title")
//                .setContentText("Alarm text")
////                .setContentIntent(
////                        PendingIntent.getActivity(this, 0, new Intent(this,
////                                        SecondActivity.class),
////                                PendingIntent.FLAG_UPDATE_CURRENT))
////                .setSound(soundUri).setSmallIcon(R.drawable.ic_launcher)
//                .build();
//        NotificationManagerCompat.from(this).notify(0, notification);
//    }


}
