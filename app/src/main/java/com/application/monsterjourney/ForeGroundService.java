package com.application.monsterjourney;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.List;
import java.util.Objects;

public class ForeGroundService extends Service implements SensorEventListener, StepListener {
    public static final String
            STEP_COUNT = "steps_counted",
            EVENT_TYPE = "event_type",
            ACTION_BROADCAST = ForeGroundService.class.getName() + "Broadcast",
            CHANNEL_ID = "ForegroundServiceChannel",
            CHANNEL_ID2 = "EventNotificationChannel";
    private StepDetector simpleStepDetector;
    private long steps, evolvesteps;
    private long eventsteps;
    private long matchmakersteps;
    private boolean eventreached;
    private PendingIntent pendingIntent;
    private AppDatabase db;
    private int currentarrayid;
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        if (Objects.equals(intent.getAction(), "StopService")) {
//            //Toast.makeText(this,"stopped", Toast.LENGTH_SHORT).show();
//            //end the service service
//            stopForeground(true);
//            //stopSelfResult(startId);
//            stopSelf();
//            //return START_NOT_STICKY;
//        }
        //call startforeground first
        createNotificationChannel(CHANNEL_ID, getText(R.string.channel_name), getString(R.string.contentext));
        createNotificationChannel(CHANNEL_ID2, getText(R.string.channel2_name), getString(R.string.channel_description));
        Intent notificationIntent = new Intent(this, MainActivity.class);
        pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            notification =
                    new Notification.Builder(this, CHANNEL_ID)
                            .setContentTitle(getText(R.string.contenttitle))
                            .setContentText(getText(R.string.contentext))
                            .setSmallIcon(R.drawable.ic_notification_logo)
                            .setTicker(getText(R.string.contentext))
                            .setOngoing(false)
                            .setContentIntent(pendingIntent)
                            .build();
        }
        else{
            notification = new NotificationCompat.Builder(this,CHANNEL_ID)
                    .setContentTitle(getText(R.string.contenttitle))
                    .setContentText(getText(R.string.contentext))
                    .setSmallIcon(R.drawable.ic_notification_logo)
                    .setTicker(getText(R.string.contentext))
                    .setOngoing(false)
                    .setContentIntent(pendingIntent)
                    .build();
        }
        startForeground(1, notification);

        // Get an instance of the SensorManager
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        assert sensorManager != null;
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);


        AsyncTask.execute(() -> {
            db = AppDatabase.getInstance(getApplicationContext());
            Journey temp = db.journeyDao().getJourney().get(0);
            // run your queries here!
            steps = temp.getTotalsteps();
            eventsteps = temp.getEventsteps();
            eventreached = temp.isEventreached();
        });

        // Notification ID cannot be 0.
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);

        //do heavy work on a background thread
        //stopSelf();
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel(String channel_id, CharSequence channel_name, String description) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_NONE;
            NotificationChannel channel = new NotificationChannel(channel_id, channel_name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * Override our step method to count steps taken with the phone
     * @param timeNs time in Nanoseconds of the step
     */
    @Override
    public void step(long timeNs) {
        AsyncTask.execute(() -> {
            db = AppDatabase.getInstance(getApplicationContext());
            Journey temp = db.journeyDao().getJourney().get(0);
            steps = temp.getTotalsteps();
            eventsteps = temp.getEventsteps();
            eventreached = temp.isEventreached();
            matchmakersteps = temp.getMatchmakersteps();
            Monster tempmonster  = db.journeyDao().getMonster().get(0);
            currentarrayid = tempmonster.getArrayid();
            boolean isbattling = temp.getIsbattling();
            long storysteps = temp.getStorysteps();

            //if we reach an event then wait until it is addressed before counting steps again
            if (!eventreached && !isbattling && matchmakersteps >0) {
                steps++;
                eventsteps--;
                evolvesteps = tempmonster.getEvolvesteps() - 1;
                //if monster is hatched then add a story step
                if(tempmonster.getHatched()){
                    storysteps--;
                    temp.setStorysteps(storysteps);
                    if(storysteps <= 0){
                        temp.setStorysteps(0);
                        if (isAppInBackground(getApplicationContext())) {
                            createNotification("Boss Found!", "You've reach the end of the map! Time to fight the boss!");
                        }
                        sendBroadcastMessage(steps);
                        db.journeyDao().update(temp);
                        return;
                    }
                }
                if (eventsteps <= 0) {
                    eventreached = true;
                    if(!tempmonster.getHatched()){
                        temp.setEventtype(0);
                    }
                    if (isAppInBackground(getApplicationContext())) {
                        switch (temp.getEventtype()) {
                            case 0: // egg hatching
                                createNotification("Egg Ready to Hatch", "You've taken enough steps to hatch your egg!");
                                break;
                            case 1: // item found
                                createNotification("Item Found", "Your monster has found an item!");
                                break;
                            case 2: //battle found
                                createNotification("Enemy Found", "Your monster encountered an enemy!");
                                break;
                        }
                    }
                }
                else if(temp.isMatching()){
                    matchmakersteps--;
                    temp.setMatchmakersteps(matchmakersteps);
                    if(matchmakersteps <= 0){
                        //eventreached = true;
                        if (isAppInBackground(getApplicationContext())) {
                            createNotification("Match found", "The Matchmaker has found a match for your monster!");
                        }
                    }
                }
                temp.setTotalsteps(steps);
                temp.setEventreached(eventreached);
                temp.setEventsteps(eventsteps);
                tempmonster.setEvolvesteps(evolvesteps);
                db.journeyDao().update(temp);
                db.journeyDao().updateMonster(tempmonster);
                //sendBroadcastMessage(matchmakersteps);
                sendBroadcastMessage(steps);
                //sendBroadcastMessage(eventreached);
            }
        });
    }

    private void sendBroadcastMessage(long steps) {
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra(STEP_COUNT, steps);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendBroadcastEvent(int eventtype){
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra(EVENT_TYPE, eventtype);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendBroadcastMessage(boolean value){
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra(STEP_COUNT, steps);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Method checks if the app is in background or not
     */
    private boolean isAppInBackground(Context context) {
        boolean isInBackground = true;

        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        assert am != null;
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                for (String activeProcess : processInfo.pkgList) {
                    if (activeProcess.equals(context.getPackageName())) {
                        isInBackground = false;
                    }
                }
            }
        }
        return isInBackground;
    }

    /**
     * create a notification for different events reached
     */
    public void createNotification(String contenttitle, String contenttext) {
        @StyleableRes int index = 4;
        //use our r.array id to find array for current monster
        TypedArray array = getBaseContext().getResources().obtainTypedArray(currentarrayid);
        int resource = array.getResourceId(index,R.drawable.egg_idle);
        array.recycle();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification notification =
                    new Notification.Builder(this, CHANNEL_ID2)
                            .setContentTitle(contenttitle)
                            .setContentText(contenttext)
                            .setSmallIcon(resource)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .setTicker(getText(R.string.channel_description))
                            .build();
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.notify(2, notification);
        } else {
            //a notification requires a channelid type for newer operating systems and its own notification id
            Notification notification =
                    new NotificationCompat.Builder(this, CHANNEL_ID2)
                            .setContentTitle(contenttitle)
                            .setContentText(contenttext)
                            .setSmallIcon(resource)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .setTicker(getText(R.string.channel_description))
                            .build();
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(2, notification);
        }
    }
}

