package com.application.monsterjourney;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ClipDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.StyleableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetailsParams;
import com.appodeal.ads.Appodeal;
import com.google.android.gms.ads.AdView;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "MyJourneyFile";
    public static final String APP_ID = "temp";
    SharedPreferences settings;

    private boolean runbackground, restartset, offlineService, isbattling; //the check for if we track steps while the app is closed

    private Monster currentmonster;
    private Intent mServiceIntent;

    public int currentarrayid, currentstoryarray;

    private NumberPicker picker;
    private Button selectedpick;

    //views used for our popups, stored in global variables so we don't make 2 popups at once
    public View aboutView, trainView, battleView, optionsView, storeView, matchView, purchaseView, aboutpopupView, hatchedView;
    public int trainingtapcount;

    private int enemyarrayid, enemyhealth, enemymaxhealth;

    private AdView mAdView;
    private View banner;
    private BillingClient billingClient;
    private StepReceiver stepReceiver;

    private MediaPlayer music;
    private int currentvolume;
    private boolean isplaying;

    private boolean initialized = false;
    boolean isbought;

    private int nexteventran = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //initialize appodeal ads
        Appodeal.initialize(this, APP_ID, Appodeal.BANNER, false);

        //Appodeal.setLogLevel(com.appodeal.ads.utils.Log.LogLevel.verbose);
        offlineService = false;
        FirstTimeCheck runner = new FirstTimeCheck(this);
        runner.execute();
        setContentView(R.layout.activity_main);
        settings = getSharedPreferences(PREFS_NAME, 0);
        //totaltime = findViewById(R.id.total_time);
        picker = findViewById(R.id.picker);
        selectedpick = findViewById(R.id.selection);
        restartset = false;

        isbought = settings.getBoolean("isbought", false);
        setupBillingClient();
        Activity mainActivity = this;
        //check if player has paid for app to remove ads, if not then initialize and load our ad
        //mAdView = findViewById(R.id.adView);
        banner = findViewById(R.id.appodealBannerView);
        banner.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(!isbought){
                    Appodeal.setBannerViewId(R.id.appodealBannerView);
                    Appodeal.set728x90Banners(true);
                    Appodeal.show(mainActivity, Appodeal.BANNER_VIEW);
//                    MobileAds.initialize(getApplicationContext(), initializationStatus -> {
//                    });
//
//                    AdRequest adRequest = new AdRequest.Builder().build();
//                    mAdView.loadAd(adRequest);

                }
                else{
//                    mAdView.pause();
//                    mAdView.setVisibility(View.GONE);
                    banner.setVisibility(View.GONE);
                }

                String[] pickervals = new String[]{"Library", "Map", "Care", "Ranch", "Minigame","Connect", "Shop"};

                picker.setMaxValue(6);
                picker.setMinValue(0);

                picker.setDisplayedValues(pickervals);

                picker.setOnValueChangedListener((numberPicker, i, i1) -> selectedpick());

                selectedpick();
                Purchase.PurchasesResult result = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
                if (result.getPurchasesList() != null) {
                    for (Purchase purchase : result.getPurchasesList()) {
                        //think this works but not sure
                        handlePurchase(purchase);
                    }
                }

                banner.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        runbackground = settings.getBoolean("runbackground", true);

        // Add code to print out the key hash
//        try {
//            PackageInfo info = getPackageManager().getPackageInfo(
//                    "your.package",
//                    PackageManager.GET_SIGNATURES);
//            for (Signature signature : info.signatures) {
//                MessageDigest md = MessageDigest.getInstance("SHA");
//                md.update(signature.toByteArray());
//                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
//            }
//        } catch (PackageManager.NameNotFoundException e) {
//
//        } catch (NoSuchAlgorithmException e) {
//
//        }

//        findViewById(R.id.btn_start).setOnClickListener(arg0 -> {
////            monster_hatched(this);
//                    AsyncTask.execute(() -> {
//                        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
////                List<UnlockedMonster> unlockedMonsters = db.journeyDao().getUnlockedMonster();
////                for(UnlockedMonster unlockedMonster : unlockedMonsters){
////                    unlockedMonster.setUnlocked(true);
////                    unlockedMonster.setDiscovered(true);
////
////                }
////
////                db.journeyDao().updateUnlockedMonster(unlockedMonsters);
//                        Journey tempjourney = db.journeyDao().getJourney().get(0);
//                        tempjourney.addStepstoJourney(100, false);
//                        Monster tempmonster = db.journeyDao().getMonster().get(0);
//                        tempmonster.setEvolvesteps(0);
////                db.journeyDao().updateMonster(tempmonster);
////                //matchmaking event
//                        //tempjourney.setEventtype(1);
////                tempjourney.setEventsteps(0);
////                tempjourney.setEventsteps(10);
//                        //tempjourney.setStorysteps(0);
//////                tempjourney.setMatching(true);
//////                tempjourney.setMatchmakersteps(0);
////                tempjourney.setEventreached(true);
//////                Monster tempmonster = db.journeyDao().getMonster().get(0);
//////                //tempmonster.setEvolvesteps(tempmonster.getEvolvesteps() - 100);
//                        db.journeyDao().update(tempjourney);
//                        db.journeyDao().updateMonster(tempmonster);
//                    });
//                });

        //button for testing
//        findViewById(R.id.test).setOnClickListener(v -> {
////            BossDefeated runner2 = new BossDefeated(this);
////            runner2.execute();
//           AsyncTask.execute(()->{
//               AppDatabase db = AppDatabase.getInstance(getApplicationContext());
//               Journey tempjourney = db.journeyDao().getJourney().get(0);
////               Monster tempmonster = db.journeyDao().getMonster().get(0);
//              // tempmonster.setEvolvesteps(0);
////               tempjourney.setMatchmakersteps(0);
////               tempjourney.setMatching(true);
//               tempjourney.setEventtype(2);
//               tempjourney.setEventsteps(0);
//               //tempjourney.setStorysteps(0);
//               //db.journeyDao().updateMonster(tempmonster);
//               //db.journeyDao().update(tempjourney);
////                               List<UnlockedMonster> unlockedMonsters = db.journeyDao().getUnlockedMonster();
////                for(UnlockedMonster unlockedMonster : unlockedMonsters){
////                    unlockedMonster.setUnlocked(true);
////                    unlockedMonster.setDiscovered(true);
////
////                }
////                List<CompletedMaps> completedMapsList = db.journeyDao().getCompletedMaps();
////                for(CompletedMaps completedMaps : completedMapsList){
////                    completedMaps.setIsunlocked(true);
////                }
//               db.journeyDao().update(tempjourney);
//
////                db.journeyDao().updateCompletedMaps(completedMapsList);
////                db.journeyDao().updateUnlockedMonster(unlockedMonsters);
//           });
//        });

        findViewById(R.id.game_options).setOnClickListener(v -> options());

        //add our home screen with the current monster
        final FrameLayout frmlayout = findViewById(R.id.placeholder);
        LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        assert aboutinflater != null;
        final View home = aboutinflater.inflate(R.layout.home_screen, (ViewGroup)null);
        Fade mFade = new Fade(Fade.IN);
        TransitionManager.beginDelayedTransition(frmlayout, mFade);

        frmlayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                frmlayout.addView(home,0);

                findViewById(R.id.monster_info_popup).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(aboutView != null || isbattling){
                            return;
                        }
                        LayoutInflater aboutinflater = (LayoutInflater)
                                getSystemService(LAYOUT_INFLATER_SERVICE);
                        assert aboutinflater != null;
                        aboutView = aboutinflater.inflate(R.layout.monster_popup, findViewById(R.id.parent), false);
                        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
                        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
                        final PopupWindow aboutWindow = new PopupWindow(aboutView, width2, height2, true);

                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            aboutWindow.setElevation(20);
                        }
                        // show the popup window
                        // which view you pass in doesn't matter, it is only used for the window token
                        aboutWindow.setAnimationStyle(R.style.PopupAnimation);
                        aboutWindow.showAtLocation(findViewById(R.id.monster_info_popup), Gravity.CENTER, 0, 0);
                        aboutView.findViewById(R.id.close).setOnClickListener(v1 -> aboutWindow.dismiss());
                        aboutWindow.setOnDismissListener(()-> aboutView = null);
                        aboutView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                MainActivity.MonsterInfo runner = new MainActivity.MonsterInfo();
                                runner.execute();
                                aboutView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }
                        });
                    }
                });
                frmlayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        AlarmManager alarmManager =
                (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        //RTC - Fires the pending intent at the specified time
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
                0, new Intent(this, HungerService.class), PendingIntent.FLAG_UPDATE_CURRENT);
        assert alarmManager != null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent);

        //enable the boot receiver so it will run if we reboot the app
        ComponentName receiver = new ComponentName(this, BootReceiver.class);
        PackageManager pm = getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        //to delete alarm
//        if (pendingIntent != null && alarmManager != null) {
//            alarmManager.cancel(pendingIntent);
//        }

        stepReceiver = new StepReceiver();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stepReceiver);
        LocalBroadcastManager.getInstance(this).registerReceiver(
                stepReceiver, new IntentFilter(ForeGroundService.ACTION_BROADCAST)
        );
    }

    /**
     * create our options popup for the game, music and stuff
     */
    public void options(){
        if(optionsView != null || isbattling){
            return;
        }
        LayoutInflater optionsinflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert optionsinflater != null;
        optionsView = optionsinflater.inflate(R.layout.options_popup,findViewById(R.id.parent), false);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow optionsWindow = new PopupWindow(optionsView, width2, height2, true);


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            optionsWindow.setElevation(20);
        }
        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        optionsWindow.setAnimationStyle(R.style.PopupAnimation);
        optionsWindow.showAtLocation(findViewById(R.id.monster_info_popup), Gravity.CENTER, 0, 0);
        optionsView.findViewById(R.id.close).setOnClickListener(v -> optionsWindow.dismiss());

        optionsWindow.setOnDismissListener(()-> optionsView = null);


        SeekBar volControl = optionsView.findViewById(R.id.volumeBar);
        volControl.setMax(100);
        volControl.setProgress(currentvolume);
        volControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                music.setVolume((float)arg1/100,(float)arg1/100);
                currentvolume = arg1;
                saveVolume("bgvolume",arg1);
            }
        });

        SwitchCompat sw = optionsView.findViewById(R.id.pedometerswitch);
        sw.setChecked(runbackground);

        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            runbackground = isChecked;
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("runbackground", runbackground);
            editor.apply();
        });

        optionsView.findViewById(R.id.aboutbutton).setOnClickListener(v ->{
            optionsWindow.dismiss();
            aboutPopup();
        });

        optionsView.findViewById(R.id.ratebutton).setOnClickListener(v -> {
            try{
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
            } catch (ActivityNotFoundException e){
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
            }
        });

        optionsView.findViewById(R.id.videobutton).setOnClickListener(v -> {
            optionsWindow.dismiss();
            startVideo(v);
        });
    }

    /**
     * create a popoup that shows information about the game
     */
    private void aboutPopup(){
        if(aboutpopupView != null){
            return;
        }
        LayoutInflater optionsinflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        assert optionsinflater != null;
        aboutpopupView = optionsinflater.inflate(R.layout.about_popup, findViewById(R.id.parent), false);
        final PopupWindow aboutWindow = new PopupWindow(aboutpopupView, width2, height2, true);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            aboutWindow.setElevation(20);
        }
        aboutWindow.setAnimationStyle(R.style.PopupAnimation);
        aboutWindow.showAtLocation(findViewById(R.id.monster_info_popup), Gravity.CENTER, 0, 0);
        aboutpopupView.findViewById(R.id.close).setOnClickListener(view-> aboutWindow.dismiss());
        aboutWindow.setOnDismissListener(()->{
            aboutpopupView = null;
            AsyncTask.execute(()->{
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                Journey tempjourney = db.journeyDao().getJourney().get(0);
                tempjourney.setShowabout(true);
                db.journeyDao().update(tempjourney);
            });
        });
    }


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }


    @Override
    protected void onStop() {
        music.release();
        //stopService();
        if(runbackground){
            if(!restartset){
                restartset = true;
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction("restartservice");
                broadcastIntent.setClass(this, Restarter.class);
                this.sendBroadcast(broadcastIntent);
            }
        } else {
            offlineService = false;
            stopService();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        music.release();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stepReceiver);
//        if(runbackground){
//            Intent broadcastIntent = new Intent();
//            broadcastIntent.setAction("restartservice");
//            broadcastIntent.setClass(this, Restarter.class);
//            this.sendBroadcast(broadcastIntent);
//        }
//        //else {
//           // Intent serviceIntent = new Intent(this, ForeGroundService.class);
//            //serviceIntent.setAction("StopService");
//            //ContextCompat.startForegroundService(this, serviceIntent);
//            //stopService();
//        //}
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        music.release();
        if(!isbought){
            Appodeal.hide(this, Appodeal.BANNER_VIEW);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        AsyncTask.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            Journey tempjourney = db.journeyDao().getJourney().get(0);
//            totaltime.setText(String.valueOf(tempjourney.getTotalsteps()));

            currentmonster = db.journeyDao().getMonster().get(0);
            currentarrayid = currentmonster.getArrayid();
            currentstoryarray = tempjourney.getStorytype();
        });
        Activity mainActivity = this;
        if(initialized){
            DisplayMonster runner = new DisplayMonster(mainActivity);
            runner.execute();
            handleEvent();
        }
        else{
            final FrameLayout frmlayout = findViewById(R.id.placeholder);
            frmlayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    DisplayMonster runner = new DisplayMonster(mainActivity);
                    runner.execute();
                    initialized = true;
                    handleEvent();
                    frmlayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        }

        Purchase.PurchasesResult result = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
        if (result.getPurchasesList() != null) {
            for (Purchase purchase : result.getPurchasesList()) {
                //think this works but not sure
                handlePurchase(purchase);
            }
        }


        //add music to our app
        int tempvolume = 80;
        music = MediaPlayer.create(MainActivity.this,R.raw.testwalk);
        music.setLooping(true);
        currentvolume = settings.getInt("bgvolume", tempvolume);

        music.setVolume((float)currentvolume/100, (float)currentvolume/100);
        isplaying = settings.getBoolean("isplaying",isplaying);
        //music.prepareAsync();

        if(!isplaying){
            music.start();
        } else {
            findViewById(R.id.sound_button).setBackgroundResource(R.drawable.ic_button_sound_off);
        }

        if (!isMyServiceRunning(ForeGroundService.class)) {
        if(mServiceIntent == null){
            //Service mForeGroundService = new ForeGroundService();
            mServiceIntent = new Intent(this, ForeGroundService.class);
                startService(mServiceIntent);
            }
            else if (!runbackground && offlineService){
                startService(mServiceIntent);
            }
        }

        if(!isbought){
            Appodeal.show(this, Appodeal.BANNER_VIEW);
        }

        super.onResume();
    }

    /**
     * toggle the sound boolean and button drawable
     * @param v the soundbutton being clicked
     */
    public void soundClick(View v){
        isplaying = !isplaying;
        if(isplaying){
            music.pause();
            v.setBackgroundResource(R.drawable.ic_button_sound_off);
        }else{
            music.start();
            v.setBackgroundResource(R.drawable.ic_button_sound);
        }
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("isplaying", isplaying);
        editor.apply();

    }

    /**
     * start our foreground step tracking service
     *
     *
     */
    public void startService() {
        Intent serviceIntent = new Intent(this, ForeGroundService.class);
        serviceIntent.putExtra("inputExtra", "Foreground Service for tracking steps");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, ForeGroundService.class));
        } else {
            startService(new Intent(this, ForeGroundService.class));
        }
        //startService(serviceIntent);
    }

    /**
     * stop our foreground step tracking service
     */
    public void stopService() {
//        Intent serviceIntent = new Intent(this, ForeGroundService.class);
//        stopService(serviceIntent);
        //Intent serviceIntent = new Intent(this, ForeGroundService.class);
        //mServiceIntent.setAction("StopService");
        //ContextCompat.startForegroundService(this, mServiceIntent);
        if(mServiceIntent == null){
            mServiceIntent = new Intent(this, ForeGroundService.class);
        }
        stopService(mServiceIntent);
        //mServiceIntent = null;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        //monsteranimator.start();
        super.onWindowFocusChanged(hasFocus);
    }

    /**
     * open the egg select menu
     */
    public void eggSelect(){
        if(music != null){
            music.release();
        }
        Intent intent = new Intent(this, EggSelect.class);
        startActivity(intent);
        //where right side is current view
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        //finish();
        //change views and close current selection, it will be re opened when re selected
    }

    /**
     * open the library view
     */
    public void library(){
        music.release();
        offlineService = true;
        Intent intent = new Intent(this, Library.class);
        startActivity(intent);
        //where right side is current view
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    /**
     * open the map
     */
    public void map(){
        music.release();
        offlineService = true;
        Intent intent = new Intent(this, Map.class);
        startActivity(intent);
        //where right side is current view
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    /**
     * open training activity
     */
    public void train(){
//        StartTraining runner = new StartTraining(this);
//        runner.execute();
        StartCheck runner = new StartCheck(this,1);
        runner.execute();
    }

    /**
     * open ranch activity
     */
    public void ranch(){
        //we check if the egg is hatched before starting this activity to avoid event status complications
//        StartRanch runner = new StartRanch(this);
//        runner.execute();
        StartCheck runner = new StartCheck(this,0);
        runner.execute();
    }

    /**
     * open minigame activity
     */
    public void minigame(){
        music.release();
        offlineService = true;
        Intent intent = new Intent(this, Minigame.class);
        startActivity(intent);
        //where right side is current view
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    /**
     * open connect activity for communicating with friends
     */
    public void connect(){
//        music.release();
//        Intent intent = new Intent(MainActivity.this, Communication.class);
//        startActivity(intent);
//        //where right side is current view
//        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        StartCheck runner = new StartCheck(this,2);
        runner.execute();
    }


    private PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
        // To be implemented in a later section.
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
            && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        }
        else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
        // Handle an error caused by a user cancelling the purchase flow.
        } else {
        // Handle any other error codes.
        }

    };

    /**
     * handle the purchase retrieved from BillingClient#queryPurchases or your PurchasesUpdatedListener
     */
    public void handlePurchase(Purchase purchase){
        AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = billingResult -> {

        };
        switch (purchase.getSku()) {
            case "remove_advertisements":
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    if (!purchase.isAcknowledged()) {
                        AcknowledgePurchaseParams acknowledgePurchaseParams =
                                AcknowledgePurchaseParams.newBuilder()
                                        .setPurchaseToken(purchase.getPurchaseToken())
                                        .build();
                        billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
                    }
                    SharedPreferences.Editor editor = settings.edit();
                    if(!settings.getBoolean("isbought",false)){
                        ValidatePurchase runner = new ValidatePurchase(this, 0);
                        runner.execute();
                        editor.putBoolean("isbought", true);
                        editor.apply();
                        if(banner != null) {
                            Appodeal.destroy(Appodeal.BANNER_VIEW);
                        }

//                        if(mAdView != null)
//                            mAdView.pause();
//                            mAdView.setVisibility(View.GONE);
//                        }
                    }


                }
                break;
            case "purchase_dark_egg":
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    if (!purchase.isAcknowledged()) {
                        AcknowledgePurchaseParams acknowledgePurchaseParams =
                                AcknowledgePurchaseParams.newBuilder()
                                        .setPurchaseToken(purchase.getPurchaseToken())
                                        .build();
                        billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
                    }
                    SharedPreferences.Editor editor2 = settings.edit();
                    editor2.putBoolean("darkisbought", true);
                    editor2.apply();
                    ValidatePurchase runner = new ValidatePurchase(this, 1);
                    runner.execute();

                }
                break;
            case "purchase_light_egg":
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    if (!purchase.isAcknowledged()) {
                        AcknowledgePurchaseParams acknowledgePurchaseParams =
                                AcknowledgePurchaseParams.newBuilder()
                                        .setPurchaseToken(purchase.getPurchaseToken())
                                        .build();
                        billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
                    }
                    SharedPreferences.Editor editor3 = settings.edit();
                    editor3.putBoolean("lightisbought", true);
                    editor3.apply();
                    ValidatePurchase runner = new ValidatePurchase(this, 2);
                    runner.execute();
                }
                break;
            case "purchase_cosmic_egg":
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    if (!purchase.isAcknowledged()) {
                        AcknowledgePurchaseParams acknowledgePurchaseParams =
                                AcknowledgePurchaseParams.newBuilder()
                                        .setPurchaseToken(purchase.getPurchaseToken())
                                        .build();
                        billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
                    }
                    SharedPreferences.Editor editor4 = settings.edit();
                    editor4.putBoolean("cosmicisbought", true);
                    editor4.apply();
                    ValidatePurchase runner = new ValidatePurchase(this, 3);
                    runner.execute();
                }
                break;
            case "purchase_item_bundle":
                ConsumeParams consumeParams =
                        ConsumeParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();

                ConsumeResponseListener listener = (billingResult, purchaseToken) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        // Handle the success of the consume operation.
                        AsyncTask.execute(() -> {
                            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                            Item[] itemlist = {
                                    new Item(2),
                                    new Item(2),
                                    new Item(2),
                                    new Item(2),
                                    new Item(2),
                                    new Item(3),
                                    new Item(3),
                                    new Item(3),
                                    new Item(3),
                                    new Item(3),
                                    new Item(4),
                                    new Item(4),
                                    new Item(4),
                                    new Item(4),
                                    new Item(4)
                            };

                            db.journeyDao().insertAllItems(itemlist);
                        });
                        purchasemadePopup(R.drawable.ic_food_display_itembundle, R.string.item_pack);
                    }
                };

                billingClient.consumeAsync(consumeParams, listener);
                break;
        }
        // Verify the purchase.
        // Ensure entitlement was not already granted for this purchaseToken.
        // Grant entitlement to the user.
    }

    public void startConnection(){
//        if(billingClient == null){
//            return;
//        }
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    billingClient.queryPurchases(BillingClient.SkuType.INAPP);
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                startConnection();
            }
        });
    }

    /**
     * opens the shop popup where players can
     */
    public void store(){
        if(storeView != null){
            return;
        }
        startConnection();

        LayoutInflater storeinflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert storeinflater != null;
        storeView = storeinflater.inflate(R.layout.store_popup, null);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow storeWindow = new PopupWindow(storeView, width2, height2, true);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            storeWindow.setElevation(20);
        }
        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        storeWindow.setAnimationStyle(R.style.PopupAnimation);
        storeWindow.showAtLocation(findViewById(R.id.monster_info_popup), Gravity.CENTER, 0, 0);
        storeView.findViewById(R.id.close).setOnClickListener(v -> storeWindow.dismiss());

        storeWindow.setOnDismissListener(() -> storeView = null);

        storeView.findViewById(R.id.buy_ad).setOnClickListener(v -> {
            List<String> skuList2 = new ArrayList<> ();
            skuList2.add("remove_advertisements");

            SkuDetailsParams.Builder params2 = SkuDetailsParams.newBuilder();
            params2.setSkusList(skuList2).setType(BillingClient.SkuType.INAPP);
            billingClient.querySkuDetailsAsync(params2.build(),
                    (billingResult, skuDetailsList) -> {
                        //can't test purchases on emulator, must be part of an alpha test track
                        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                .setSkuDetails(skuDetailsList.get(0))
                                .build();
                        int responseCode = billingClient.launchBillingFlow(this, billingFlowParams).getResponseCode();
                    });
        });

        storeView.findViewById(R.id.buydarkegg).setOnClickListener(v -> {
            List<String> skuList2 = new ArrayList<> ();
            skuList2.add("purchase_dark_egg");

            SkuDetailsParams.Builder params2 = SkuDetailsParams.newBuilder();
            params2.setSkusList(skuList2).setType(BillingClient.SkuType.INAPP);
            billingClient.querySkuDetailsAsync(params2.build(),
                    (billingResult, skuDetailsList) -> {
                        //can't test purchases on emulator, must be part of an alpha test track
                        assert skuDetailsList != null;
                        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                .setSkuDetails(skuDetailsList.get(0))
                                .build();
                        int responseCode = billingClient.launchBillingFlow(this, billingFlowParams).getResponseCode();
                    });

        });

        storeView.findViewById(R.id.buylightegg).setOnClickListener(v -> {
            List<String> skuList2 = new ArrayList<> ();
            skuList2.add("purchase_light_egg");

            SkuDetailsParams.Builder params2 = SkuDetailsParams.newBuilder();
            params2.setSkusList(skuList2).setType(BillingClient.SkuType.INAPP);
            billingClient.querySkuDetailsAsync(params2.build(),
                    (billingResult, skuDetailsList) -> {
                        //can't test purchases on emulator, must be part of an alpha test track
                        assert skuDetailsList != null;
                        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                .setSkuDetails(skuDetailsList.get(0))
                                .build();
                        int responseCode = billingClient.launchBillingFlow(this, billingFlowParams).getResponseCode();
                    });

        });

        storeView.findViewById(R.id.buycosmicegg).setOnClickListener(v -> {
            List<String> skuList2 = new ArrayList<> ();
            skuList2.add("purchase_cosmic_egg");

            SkuDetailsParams.Builder params2 = SkuDetailsParams.newBuilder();
            params2.setSkusList(skuList2).setType(BillingClient.SkuType.INAPP);
            billingClient.querySkuDetailsAsync(params2.build(),
                    (billingResult, skuDetailsList) -> {
                        //can't test purchases on emulator, must be part of an alpha test track
                        assert skuDetailsList != null;
                        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                .setSkuDetails(skuDetailsList.get(0))
                                .build();
                        int responseCode = billingClient.launchBillingFlow(this, billingFlowParams).getResponseCode();
                    });
        });

        storeView.findViewById(R.id.buyitempack).setOnClickListener(v -> {
            List<String> skuList2 = new ArrayList<> ();
            skuList2.add("purchase_item_bundle");

            SkuDetailsParams.Builder params2 = SkuDetailsParams.newBuilder();
            params2.setSkusList(skuList2).setType(BillingClient.SkuType.INAPP);
            billingClient.querySkuDetailsAsync(params2.build(),
                    (billingResult, skuDetailsList) -> {
                        // Process the result.
                        //can't test purchases on emulator, must be part of an alpha test track
                        assert skuDetailsList != null;
                        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                .setSkuDetails(skuDetailsList.get(0))
                                .build();
                        int responseCode = billingClient.launchBillingFlow(this, billingFlowParams).getResponseCode();
                    });

        });

        storeView.findViewById(R.id.restorepurchase).setOnClickListener(v->{
            Purchase.PurchasesResult result = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
            if (result.getPurchasesList() != null) {
                for (Purchase purchase : result.getPurchasesList()) {
                    handlePurchase(purchase);
                }
            }
            //billingClient.launchBillingFlow(this, result.get)
        });

        if(settings.getBoolean("isbought", false)){
            storeView.findViewById(R.id.buy_ad_container).setVisibility(View.GONE);
        }
        if(settings.getBoolean("darkisbought", false)){
            storeView.findViewById(R.id.dark_egg_container).setVisibility(View.GONE);
        }
        if(settings.getBoolean("lightisbought", false)){
            storeView.findViewById(R.id.light_egg_container).setVisibility(View.GONE);
        }
        if(settings.getBoolean("cosmicisbought", false)){
            storeView.findViewById(R.id.cosmic_egg_container).setVisibility(View.GONE);
        }
    }

    /**
     * set up the billing client used for purchases
     */
    private void setupBillingClient(){
        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases()
                .setListener(purchasesUpdatedListener)
                .build();
    }


    /**
     * shows the icon for our current monster
     * @param selected the selected icon for our monster
     */
    private void selectedIcon(int selected, Activity activity){
        @StyleableRes int index = 4;
        //use our r.array id to find array for current monster
        TypedArray array = activity.getBaseContext().getResources().obtainTypedArray(selected);
        TypedArray array2 = activity.getBaseContext().getResources().obtainTypedArray(currentstoryarray);
        int resource = array.getResourceId(index,R.drawable.egg_idle);
        ImageView imageView = findViewById(R.id.monster_icon);
        imageView.setImageDrawable(ContextCompat.getDrawable(activity.getApplicationContext(), resource));
        int background = array2.getResourceId(index,R.drawable.egg_idle);
        findViewById(R.id.back_screen).setBackgroundResource(background);

        AnimationDrawable monsteranimator = (AnimationDrawable) imageView.getDrawable();
        monsteranimator.start();
        array.recycle();
        array2.recycle();
    }

    /**
     * animate the monster happy animation
     */
    private void happyanimation(Activity activity){
        TypedArray array = activity.getBaseContext().getResources().obtainTypedArray(currentarrayid);
        @StyleableRes int index = 4;
        int[] monsterresources = getResources().getIntArray(currentarrayid);
        int evolutions = monsterresources[1];
        int happyresource = array.getResourceId(index+evolutions+10, R.drawable.egg_idle);
        ImageView imageView = activity.findViewById(R.id.monster_icon);
        imageView.setImageDrawable(ContextCompat.getDrawable(activity.getApplicationContext(), happyresource));
        AnimationDrawable monsteranimator = (AnimationDrawable) imageView.getDrawable();
        monsteranimator.start();
        array.recycle();
    }

    /**
     * animate the monster attack animation
     */
    private void attackanimation(Activity activity){
        TypedArray array = activity.getBaseContext().getResources().obtainTypedArray(currentarrayid);
        @StyleableRes int index = 4;
        int[] monsterresources = getResources().getIntArray(currentarrayid);
        int evolutions = monsterresources[1];
        int resource = array.getResourceId(index+evolutions+9, R.drawable.egg_idle);
        ImageView imageView = activity.findViewById(R.id.monster_icon);
        imageView.setImageDrawable(ContextCompat.getDrawable(activity.getApplicationContext(), resource));
        AnimationDrawable monsteranimator = (AnimationDrawable) imageView.getDrawable();
        monsteranimator.start();
        array.recycle();
    }

    /**
     * handles steps taken for our event
     */
    private void handleEvent(){
        AsyncResume runner = new AsyncResume(this);
        runner.execute();
    }

    /**
     * code for when our monster has reached an event of some sort
     * @param eventtype the type of event that happened, egg hatching, battle/item found etc.
     */
    public void startEvent(int eventtype, Activity activity){
        ImageView eventimage = findViewById(R.id.monster_event);
        eventimage.setVisibility(View.VISIBLE);
        final Animation eventanimation = new AlphaAnimation(1, 0); //to change visibility from visible to invisible
        eventanimation.setDuration(1000); //1 second duration for each animation cycle
        eventanimation.setInterpolator(new LinearInterpolator());
        eventanimation.setRepeatCount(Animation.INFINITE); //repeating indefinitely
        eventanimation.setRepeatMode(Animation.REVERSE); //animation will start from end point once ended.
        eventimage.startAnimation(eventanimation); //to start animation
        final Button BtnEvent = activity.findViewById(R.id.event);
        BtnEvent.setOnClickListener(null);
        switch(eventtype){
            case 0: //egg hatching
                BtnEvent.setOnClickListener(v -> {
                    eventimage.setVisibility(View.INVISIBLE);
                    AsyncTask.execute(() -> {
                        AppDatabase db = AppDatabase.getInstance(activity);
                        Monster tempmonster = db.journeyDao().getMonster().get(0);
                        Journey temp = db.journeyDao().getJourney().get(0);
                        tempmonster.hatch();
                        tempmonster.evolve(activity, temp.getEvolveddiscount());
                        db.journeyDao().updateMonster(tempmonster);
                        currentmonster = tempmonster;
                        Random ran = new Random();
                        temp.setEventsteps(ran.nextInt(nexteventran) + 500);
                        if(ran.nextFloat() < 0.8){
                            temp.setEventtype(2);
                        }
                        else{
                            temp.setEventtype(1);
                        }
                        temp.setEventreached(false);
                        db.journeyDao().update(temp);

                        List<UnlockedMonster> unlockedMonsters = db.journeyDao().getUnlockedMonster();
                        for(UnlockedMonster unlockedMonster : unlockedMonsters){
                            if(unlockedMonster.getMonsterarrayid() == tempmonster.getArrayid()){
                                unlockedMonster.setUnlocked(true);
                                unlockedMonster.setDiscovered(true);
                            }
                        }
                        db.journeyDao().updateUnlockedMonster(unlockedMonsters);
                    });
                    eventanimation.cancel();
                    monster_hatched(activity);
                    selectedIcon(currentmonster.getArrayid(), activity);
                    BtnEvent.setVisibility(View.INVISIBLE);
                });

                BtnEvent.setText(getText(R.string.evolvefound));
                BtnEvent.setVisibility(View.VISIBLE);
                break;
            case 1: //item found
                BtnEvent.setOnClickListener(v -> {
                    Random ran = new Random();
                    Item tempitem= new Item(ran.nextInt(3)+1);

                    eventimage.setVisibility(View.INVISIBLE);
                    eventanimation.cancel();
                    AsyncTask.execute(() -> {
                        AppDatabase db = AppDatabase.getInstance(activity);
                        Journey temp = db.journeyDao().getJourney().get(0);
                        temp.setEventsteps(ran.nextInt(nexteventran) + 500);
                        if(ran.nextFloat() < 0.8){
                            temp.setEventtype(2);
                        }
                        else{
                            temp.setEventtype(1);
                        }
                        temp.setEventreached(false);
                        db.journeyDao().update(temp);
                        //generate item of type 1-3
                        db.journeyDao().insertItem(tempitem);
                    });
                    @StyleableRes int index = 4;
                    FrameLayout frmlayout = findViewById(R.id.placeholder);
                    LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                    assert aboutinflater != null;
                    final View food = aboutinflater.inflate(R.layout.feeding_screen, (ViewGroup)null);
                    TypedArray maparray = activity.getBaseContext().getResources().obtainTypedArray(currentstoryarray);
                    int resource = maparray.getResourceId(index,R.drawable.basic_background);
                    maparray.recycle();
                    food.setBackground(ContextCompat.getDrawable(getApplicationContext(), resource));
                    Fade mFade = new Fade(Fade.IN);
                    TransitionManager.beginDelayedTransition(frmlayout, mFade);
                    frmlayout.removeAllViews();
                    frmlayout.addView(food,0);
                    food.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            ImageView foodimageView = food.findViewById(R.id.eating_icon);
                            switch(tempitem.getitem()){
                                case 1:
                                    foodimageView.setBackgroundResource(R.drawable.food1_eat);
                                    break;
                                case 2:
                                    foodimageView.setBackgroundResource(R.drawable.food2_eat);
                                    break;
                                case 3:
                                    foodimageView.setBackgroundResource(R.drawable.food3_eat);
                                    break;
                            }
                            ValueAnimator monsterwalk = ValueAnimator.ofFloat(0.0f,1.0f);
                            monsterwalk.setInterpolator(new LinearInterpolator());
                            monsterwalk.setDuration(2500L);

                            ImageView monster = food.findViewById(R.id.monster_icon);

                            //use our r.array id to find array for current monster
                            TypedArray array = activity.getBaseContext().getResources().obtainTypedArray(currentarrayid);
                            int resource = array.getResourceId(index,R.drawable.egg_idle);
                            int[] monsterresources = getResources().getIntArray(currentarrayid);
                            int evolutions = monsterresources[1];
                            int happyresource = array.getResourceId(index+evolutions+10, R.drawable.egg_idle);

                            monster.setBackgroundResource(resource);
                            AnimationDrawable temp = (AnimationDrawable)monster.getBackground();
                            temp.start();
                            monster.setVisibility(View.INVISIBLE);
                            monsterwalk.addUpdateListener(animation -> {
                                monster.setVisibility(View.VISIBLE);

                                monster.setScaleX(1);
                                final float progress = (float) animation.getAnimatedValue();
                                float width = food.getWidth()*progress;
                                monster.setTranslationX(-width+food.getWidth());

                            });
                            monsterwalk.addListener(new AnimatorListenerAdapter()
                            {
                                @Override
                                public void onAnimationEnd(Animator animation)
                                {
                                    temp.stop();
                                    monster.setBackgroundResource(happyresource);
                                    AnimationDrawable temp = (AnimationDrawable)monster.getBackground();
                                    temp.start();
                                    Handler h = new Handler();
                                    //Run a runnable to switch view back to homeview
                                    h.postDelayed(() ->{
                                                final View home = aboutinflater.inflate(R.layout.home_screen, (ViewGroup)null);
                                                Fade mFade = new Fade(Fade.IN);
                                                TransitionManager.beginDelayedTransition(frmlayout, mFade);
                                                frmlayout.removeAllViews();
                                                frmlayout.addView(home,0);
                                                selectedIcon(currentarrayid, activity);
                                    }, 3000);
                                }
                            });
                            monsterwalk.start();
                            array.recycle();

                            food.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    });
                    BtnEvent.setVisibility(View.INVISIBLE);
                });
                BtnEvent.setText(getText(R.string.itemfound));
                BtnEvent.setVisibility(View.VISIBLE);

                happyanimation(activity);
                break;

            case 2: //battle found
                BtnEvent.setOnClickListener(v -> {
                    BtnEvent.setVisibility(View.INVISIBLE);
                    eventimage.setVisibility(View.INVISIBLE);
                    eventanimation.cancel();
                    isbattling = true;
                    prepareBattle(true, false);

                });
                BtnEvent.setText(getText(R.string.enemyfound));
                BtnEvent.setVisibility(View.VISIBLE);

                attackanimation(activity);
                break;
            case 3://match found
                BtnEvent.setText(getText(R.string.matchfound));
                BtnEvent.setVisibility(View.VISIBLE);
                happyanimation(activity);
                BtnEvent.setOnClickListener(v -> {
                    eventimage.setVisibility(View.INVISIBLE);
                    eventanimation.cancel();
                    BtnEvent.setVisibility(View.INVISIBLE);
                    //popup display name/type of monster
                    //If we breed, retire current monster and replace with new egg
                    //add monster to discovered list even if we choose not to breed with them
                    Random ran = new Random();
                    int selectedarray = ran.nextInt(6);
                    TypedArray matcharray = activity.getResources().obtainTypedArray(R.array.matchmaker_list);
                    int matchedarray = matcharray.getResourceId(selectedarray, R.array.dino_baby1);

                    @StyleableRes int matchindex = 4;
                    //use our suitorarray id to find drawable for current monster
                    TypedArray suitorarray = getApplicationContext().getResources().obtainTypedArray(matchedarray);
                    int suitorresource = suitorarray.getResourceId(matchindex,R.drawable.egg_idle);
                    suitorarray.recycle();
                    matcharray.recycle();
                    AsyncTask.execute(()->{
                        AppDatabase db = AppDatabase.getInstance(this);
                        List<UnlockedMonster> unlockedMonsters = db.journeyDao().getUnlockedMonster();
                        for(UnlockedMonster unlockedMonster : unlockedMonsters){
                            if(unlockedMonster.getMonsterarrayid() == matchedarray){
                                unlockedMonster.setDiscovered(true);
                            }
                        }
                        db.journeyDao().updateUnlockedMonster(unlockedMonsters);
                    });

                    FrameLayout frmlayout = findViewById(R.id.placeholder);
                    LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                    assert aboutinflater != null;
                    final View match = aboutinflater.inflate(R.layout.matchmaking_screen, (ViewGroup)null);
                    TypedArray maparray = activity.getBaseContext().getResources().obtainTypedArray(currentstoryarray);
                    @StyleableRes int index = 4;
                    int resource = maparray.getResourceId(index,R.drawable.basic_background);
                    maparray.recycle();
                    match.setBackground(ContextCompat.getDrawable(getApplicationContext(), resource));
                    Fade mFade = new Fade(Fade.IN);
                    TransitionManager.beginDelayedTransition(frmlayout, mFade);
                    frmlayout.removeAllViews();
                    frmlayout.addView(match,0);
                    match.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            //animate the icon for out suitor
                            ImageView suitorimageView = match.findViewById(R.id.suitor_icon);
                            suitorimageView.setBackgroundResource(suitorresource);
                            AnimationDrawable suitortemp = (AnimationDrawable)suitorimageView.getBackground();
                            suitortemp.start();

                            ValueAnimator monsterwalk = ValueAnimator.ofFloat(0.0f,1.0f);
                            monsterwalk.setInterpolator(new LinearInterpolator());
                            monsterwalk.setDuration(2000L);

                            ImageView monster = match.findViewById(R.id.monster_icon);
                            @StyleableRes int index = 4;
                            //use our r.array id to find array for current monster
                            TypedArray array = activity.getBaseContext().getResources().obtainTypedArray(currentarrayid);
                            int resource = array.getResourceId(index,R.drawable.egg_idle);
                            array.recycle();
                            monster.setBackgroundResource(resource);
                            AnimationDrawable temp = (AnimationDrawable)monster.getBackground();
                            temp.start();

                            monster.setVisibility(View.INVISIBLE);
                            suitorimageView.setVisibility(View.INVISIBLE);
                            monsterwalk.addUpdateListener(animation -> {
                                monster.setVisibility(View.VISIBLE);
                                suitorimageView.setVisibility(View.VISIBLE);

                                monster.setScaleX(1);
                                final float progress = (float) animation.getAnimatedValue();
                                float width = match.getWidth()*progress;
                                monster.setTranslationX(-width+match.getWidth()*1.3f);
                                suitorimageView.setTranslationX(width-match.getWidth()*1.3f);
                            });

                            monsterwalk.start();
                            ImageView heartsevent = match.findViewById(R.id.matched_event);
                            matchmaker_popup(matchedarray, heartsevent);


                            match.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                        });

                });
                break;
            case 4: //boss found
                attackanimation(activity);
                BtnEvent.setText(getText(R.string.bossfound));
                BtnEvent.setVisibility(View.VISIBLE);
                BtnEvent.setOnClickListener(v -> {
                    BtnEvent.setVisibility(View.INVISIBLE);
                    eventimage.setVisibility(View.INVISIBLE);
                    eventanimation.cancel();
                    isbattling = true;
                    prepareBattle(true, true);
                });
                break;
        }

    }

    /**
     * Create popup where we decide if we want to match with the monster found or not
     * @param matchedarray the array of the potential match we have found
     */
    private void matchmaker_popup(int matchedarray, ImageView hearts){
        if(matchView != null){
            return;
        }
        AtomicBoolean accepted = new AtomicBoolean(false);
        LayoutInflater confirminflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert confirminflater != null;
        matchView = confirminflater.inflate(R.layout.matchmaker_popup, null);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow matchWindow = new PopupWindow(matchView, width2, height2, true);
        matchWindow.setOutsideTouchable(false);


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            matchWindow.setElevation(20);
        }
        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        matchWindow.setAnimationStyle(R.style.PopupAnimation);
        matchWindow.showAtLocation(findViewById(R.id.game_options), Gravity.CENTER, 0, 0);
        matchView.findViewById(R.id.close).setOnClickListener(v -> matchWindow.dismiss());
        matchWindow.setOnDismissListener(() -> {
            FrameLayout frmlayout = findViewById(R.id.placeholder);
            LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            assert aboutinflater != null;
            final View home = aboutinflater.inflate(R.layout.home_screen, (ViewGroup)null);
            Fade mFade = new Fade(Fade.IN);
            TransitionManager.beginDelayedTransition(frmlayout, mFade);
            frmlayout.removeAllViews();
            frmlayout.addView(home,0);
            Activity homeactivity = this;
            home.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    selectedIcon(currentmonster.getArrayid(), homeactivity);
                    home.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });

            if(!accepted.get()){
                AsyncTask.execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                    Journey temp = db.journeyDao().getJourney().get(0);
                    temp.setMatchmakersteps(2000);
                    db.journeyDao().update(temp);
                });
            }
            matchView = null;
        });

        ImageView tempmatchmaker = matchView.findViewById(R.id.matchmaker_popup_icon);

        tempmatchmaker.setBackgroundResource(R.drawable.matchmaker_idle);
        AnimationDrawable matchanimator = (AnimationDrawable) tempmatchmaker.getBackground();
        matchanimator.start();

        TextView matchtext = matchView.findViewById(R.id.confimation_text);
        matchtext.setText(getText(R.string.MatchmakerFound));

        matchView.findViewById(R.id.confirm).setOnClickListener(v -> {
            accepted.set(true);
            int selectedid = currentmonster.breed(matchedarray, getApplicationContext());
            AsyncTask.execute(()->{
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                //update retirement info

                // reset journey info
                Journey temp = db.journeyDao().getJourney().get(0);
                temp.setEventsteps(100);
                temp.setEventtype(0);
                temp.setMatchmakersteps(2000);
                temp.setEventreached(false);
                temp.setMatching(false);
                db.journeyDao().update(temp);

                //add egg to unlocked list
                List<UnlockedMonster> unlockedMonsters = db.journeyDao().getUnlockedMonster();
                for(UnlockedMonster unlockedMonster : unlockedMonsters){
                    if(unlockedMonster.getMonsterarrayid() == selectedid){
                        unlockedMonster.setDiscovered(true);
                        unlockedMonster.setUnlocked(true);
                        break;
                    }
                }
                db.journeyDao().updateUnlockedMonster(unlockedMonsters);
                //add extra monster to party if ranch is not full
                List<Monster> monsterList = db.journeyDao().getMonster();
                if(monsterList.size() < 5){
                    db.journeyDao().insertMonster(Monster.populateData().updateMonster(currentmonster));
                }
                else {
                    History temphistory = new History(currentmonster.getGeneration(), currentmonster.getArrayid(), currentmonster.getName());
                    db.journeyDao().insertHistory(temphistory);
                }
                //create new egg
                currentmonster.newEgg(selectedid);
                db.journeyDao().updateMonster(currentmonster);
            });
            hearts.setVisibility(View.VISIBLE);
            final Animation eventanimation = new AlphaAnimation(1, 0); //to change visibility from visible to invisible
            eventanimation.setDuration(1000); //1 second duration for each animation cycle
            eventanimation.setInterpolator(new LinearInterpolator());
            eventanimation.setRepeatCount(Animation.INFINITE); //repeating indefinitely
            eventanimation.setRepeatMode(Animation.REVERSE); //animation will start from end point once ended.
            hearts.startAnimation(eventanimation); //to start animation
            Handler h = new Handler();
            //Run a runnable to hide food after it has been eaten
            h.postDelayed(matchWindow::dismiss, 2000);

        });

        matchView.findViewById(R.id.back).setOnClickListener(v -> matchWindow.dismiss());
    }

    /**
     * code for entering name data when monster was hatched
     */
    private void monster_hatched(Activity activity){
        final LayoutInflater aboutinflater = (LayoutInflater)
                activity.getSystemService(LAYOUT_INFLATER_SERVICE);
        assert aboutinflater != null;
        hatchedView = aboutinflater.inflate(R.layout.hatched_popup, activity.findViewById(R.id.parent), false);
        hatchedView.setId(View.generateViewId());
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow aboutWindow = new PopupWindow(hatchedView, width2, height2, true);
        aboutWindow.setOutsideTouchable(false);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            aboutWindow.setElevation(20);
        }
        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        aboutWindow.setAnimationStyle(R.style.PopupAnimation);
        aboutWindow.showAtLocation(activity.findViewById(R.id.monster_info_popup), Gravity.CENTER, 0, 0);
        hatchedView.findViewById(R.id.close).setOnClickListener(v -> aboutWindow.dismiss());
        EditText textbox = hatchedView.findViewById(R.id.name_enter);
        hatchedView.findViewById(R.id.name_button).setOnClickListener(v -> {
            final String monstername = textbox.getText().toString();
            AsyncTask.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(activity);
                Monster temp = db.journeyDao().getMonster().get(0);
                temp.setName(monstername);
                db.journeyDao().updateMonster(temp);
            });
            aboutWindow.dismiss();
        });
        hatchedView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                MainActivity.HatchedInfo runner = new MainActivity.HatchedInfo(activity);
                runner.execute();
                hatchedView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }


    /**
     * Popup for when a boss is defeated
     * @param awardtype if the map has been completed before, or if evolution discount earned
     * @param award the arrayid of egg awarded
     */
    private void bossDefeatPopup(int awardtype, int award){
        final LayoutInflater aboutinflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert aboutinflater != null;
        View defeatView = aboutinflater.inflate(R.layout.bossdefeat_popup, findViewById(R.id.parent), false);
        defeatView.setId(View.generateViewId());
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow defeatWindow = new PopupWindow(defeatView, width2, height2);
        defeatWindow.setOutsideTouchable(false);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            defeatWindow.setElevation(20);
        }
        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        defeatWindow.setAnimationStyle(R.style.PopupAnimation);
        defeatWindow.showAtLocation(findViewById(R.id.monster_info_popup), Gravity.CENTER, 0, 0);

        defeatView.findViewById(R.id.new_map).setOnClickListener((v) -> {
            defeatWindow.dismiss();
            map();
        });
        TextView textView = defeatView.findViewById(R.id.message_text);
        textView.setText(awardtype);

        defeatView.findViewById(R.id.close).setOnClickListener((v)->defeatWindow.dismiss());
        defeatView.findViewById(R.id.restart).setOnClickListener((v)->defeatWindow.dismiss());

        ImageView reward = defeatView.findViewById(R.id.reward_icon);
        reward.setImageDrawable(ContextCompat.getDrawable(this, award));
    }

    /**
     * generate the popup for when a purchase has been processed
     * @param drawable id for the drawable
     * @param message id for the purchase message
     */
    private void purchasemadePopup(int drawable , int message){
        if(purchaseView != null){
            return;
        }
        final LayoutInflater aboutinflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert aboutinflater != null;
        purchaseView = aboutinflater.inflate(R.layout.purchase_made_popup, findViewById(R.id.parent), false);
        purchaseView.setId(View.generateViewId());
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow defeatWindow = new PopupWindow(purchaseView, width2, height2, true);
        defeatWindow.setOutsideTouchable(false);
        defeatWindow.setOnDismissListener(()->purchaseView = null);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            defeatWindow.setElevation(20);
        }
        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        defeatWindow.setAnimationStyle(R.style.PopupAnimation);
        defeatWindow.showAtLocation(findViewById(R.id.monster_info_popup), Gravity.CENTER, 0, 0);

        TextView textView = purchaseView.findViewById(R.id.message_text);
        textView.setText(message);

        purchaseView.findViewById(R.id.close).setOnClickListener((v)->defeatWindow.dismiss());

        ImageView reward = purchaseView.findViewById(R.id.reward_icon);
        reward.setImageDrawable(ContextCompat.getDrawable(this, drawable));
    }

    /**
     * set the button info for which number picker scrolled to
     */
    private void selectedpick(){
        final int valuePicker = picker.getValue();
        String temp = picker.getDisplayedValues()[valuePicker];
        selectedpick.setText(temp);
        selectedpick.setOnClickListener(v -> {
            if (isbattling) {
                return;
            }
            switch(valuePicker){
                case 0:
                    library();
                    break;
                case 1:
                    map();
                    break;
                case 2:
                    train();
                    break;
                case 3:
                    ranch();
                    break;
                case 4:
                    minigame();
                    break;
                case 5:
                    connect();
                    break;
                case 6:
                    StorePopup runner = new StorePopup(this);
                    runner.execute();
                    break;
            }
        });
    }

    /**
     * training popup to gather energy which prepares for the battle
     */
    public void prepareBattle(final boolean first, boolean boss){
        if(trainView != null){
            return;
        }
        if(first){
            currentmonster.initializebattlestats(getApplicationContext());
            TypedArray array = getResources().obtainTypedArray(currentstoryarray);
            int[] monsterresources = getResources().getIntArray(currentmonster.getArrayid());
            int stage = monsterresources[0];
            if(!boss){
                Random ran = new Random();
                TypedArray foundenemylist = getResources().obtainTypedArray(array.getResourceId(stage-1, R.array.basic_dino_enemies));
                int[] possibleenemies = getResources().getIntArray(array.getResourceId(stage-1, R.array.child_dino_enemies));
                enemyarrayid = foundenemylist.getResourceId(ran.nextInt(possibleenemies[0])+1, R.array.dino_baby1);
                foundenemylist.recycle();
            }
            else{
                @StyleableRes int bossindex = 3;
                enemyarrayid = array.getResourceId(bossindex, R.array.dino_adult_horneddino);
            }
            array.recycle();
            int[] enemyfound = getResources().getIntArray(enemyarrayid);
            enemyhealth = enemyfound[5+enemyfound[1]] - 1;
            enemymaxhealth = enemyfound[5+enemyfound[1]] - 1;

            @StyleableRes int index = 4;
            //use our r.array id to find array for current monster
            TypedArray array1 = getApplicationContext().getResources().obtainTypedArray(enemyarrayid);
            int[] enemyresources = getResources().getIntArray(enemyarrayid);
            int evolutions = enemyresources[1];
            int resource = array1.getResourceId(index+evolutions+9, R.drawable.egg_idle);

            final Animation eventanimation = new AlphaAnimation(1, 0); //to change visibility from visible to invisible
            eventanimation.setDuration(500); //1 second duration for each animation cycle
            //eventanimation.setInterpolator(new LinearInterpolator());
            eventanimation.setRepeatCount(Animation.INFINITE); //repeating indefinitely
            eventanimation.setRepeatMode(Animation.REVERSE); //animation will start from end point once ended.
            ImageView monsterimage = findViewById(R.id.monster_icon);
            monsterimage.setScaleX(-1);
            monsterimage.setBackground(null);
            monsterimage.setImageDrawable(ContextCompat.getDrawable(this,resource));
            //monsterimage.setBackground(ContextCompat.getDrawable(this,player1));
            monsterimage.startAnimation(eventanimation); //to start animation

            array1.recycle();

            Handler h = new Handler();
            h.postDelayed(()->{
                eventanimation.cancel();
                monsterimage.setScaleX(1);
                selectedIcon(currentmonster.getArrayid(), this);
            }, 1500);

            AsyncTask.execute(()->{
                AppDatabase db = AppDatabase.getInstance(this);
                List<UnlockedMonster> unlockedMonsters = db.journeyDao().getUnlockedMonster();
                for(UnlockedMonster unlockedMonster: unlockedMonsters){
                    if(unlockedMonster.getMonsterarrayid() == enemyarrayid){
                        unlockedMonster.setDiscovered(true);
                        break;
                    }
                }
                db.journeyDao().updateUnlockedMonster(unlockedMonsters);
            });

        }
        LayoutInflater traininflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert traininflater != null;
        trainView = traininflater.inflate(R.layout.training_game, null);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow trainWindow = new PopupWindow(trainView, width2, height2, false);
        trainWindow.setOutsideTouchable(false);


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            trainWindow.setElevation(20);
        }
        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        trainWindow.setAnimationStyle(R.style.PopupAnimation);
        trainWindow.showAtLocation(findViewById(R.id.parent), Gravity.CENTER, 0, 0);
        final TextView trainingtitle = trainView.findViewById(R.id.training_title);
        final TextView trainingtime = trainView.findViewById(R.id.training_time);
        trainingtitle.setText(getText(R.string.TrainingReady));
        trainingtapcount = 0;

        int[] enemyfound = getResources().getIntArray(enemyarrayid);
        int enemyattack = enemyfound[6+enemyfound[1]];
        int enemychance = enemyfound[7+enemyfound[1]];
        AsyncTask.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            Journey temp = db.journeyDao().getJourney().get(0);
            temp.setEnemyarrayid(enemyarrayid);
            temp.setEnemyhealth(enemyhealth);
            temp.setEnemymaxhealth(enemymaxhealth);
            temp.setIsbattling(true);
            temp.setBossfight(boss);
            db.journeyDao().update(temp);
            db.journeyDao().updateMonster(currentmonster);
        });

        Handler h = new Handler();
        //Run a runnable after 100ms (after that time it is safe to remove the view)
        h.postDelayed(() -> {
            trainingtitle.setText(getText(R.string.TrainingStart));
            startTrain(trainView);
            new CountDownTimer(4000, 100) {
                // 500 means, onTick function will be called at every 500 milliseconds

                @Override
                public void onTick(long leftTimeInMilliseconds) {

                    float seconds = leftTimeInMilliseconds / 1000f;
                    //String timetext = String.format(Locale.getDefault(),"%02d.%2f", seconds % 60)+ " s";
                    String timetext = String.format(Locale.getDefault(), "%.2f", seconds)+ " s";
                    trainingtime.setText(timetext);
                }

                @Override
                public void onFinish() {
                    String timetext =  "0 s";
                    trainingtime.setText(timetext);

                    Handler h1 = new Handler();
                    //Run a runnable after 100ms (after that time it is safe to remove the view)
                    h1.postDelayed(() -> {
                        trainWindow.dismiss();
                        trainView = null;
                        performbattle(currentmonster.battle(enemyattack,enemyhealth,enemychance, trainingtapcount, false),
                                enemyattack, boss);
                    }, 2000);
                }
            }.start();
        }, 1500);
    }

    /**
     * loop for our training game
     */
    public void startTrain(final View trainView){
        ConstraintLayout templayout = trainView.findViewById(R.id.game_spot);
        final ImageView temptouch = trainView.findViewById(R.id.training_layout);
        int ranx = new Random().nextInt(3)+1;
        int rany = new Random().nextInt(3)+1;

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(templayout);
        switch(ranx){
            case 0:
                constraintSet.connect(R.id.training_layout,ConstraintSet.START,R.id.vguideline0,ConstraintSet.START,0);
                break;
            case 1:
                constraintSet.connect(R.id.training_layout,ConstraintSet.START,R.id.vguideline1,ConstraintSet.START,0);
                break;
            case 2:
                constraintSet.connect(R.id.training_layout,ConstraintSet.START,R.id.vguideline2,ConstraintSet.START,0);
                break;
            case 3:
                constraintSet.connect(R.id.training_layout,ConstraintSet.START,R.id.vguideline3,ConstraintSet.START,0);
                break;
            case 4:
                constraintSet.connect(R.id.training_layout,ConstraintSet.START,R.id.vguideline4,ConstraintSet.START,0);
                break;
        }

        switch(rany){
            case 0:
                constraintSet.connect(R.id.training_layout,ConstraintSet.TOP,R.id.guideline0,ConstraintSet.TOP,0);
                break;
            case 1:
                constraintSet.connect(R.id.training_layout,ConstraintSet.TOP,R.id.guideline1,ConstraintSet.TOP,0);
                break;
            case 2:
                constraintSet.connect(R.id.training_layout,ConstraintSet.TOP,R.id.guideline2,ConstraintSet.TOP,0);
                break;
            case 3:
                constraintSet.connect(R.id.training_layout,ConstraintSet.TOP,R.id.guideline3,ConstraintSet.TOP,0);
                break;
            case 4:
                constraintSet.connect(R.id.training_layout,ConstraintSet.TOP,R.id.guideline4,ConstraintSet.TOP,0);
                break;
        }
        constraintSet.applyTo(templayout);

        temptouch.setVisibility(View.VISIBLE);
        Animation popupanimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.training_popup);
        temptouch.startAnimation(popupanimation);
        temptouch.setOnClickListener(v -> {
            temptouch.setVisibility(View.INVISIBLE);
            ImageView temp2 = trainView.findViewById(R.id.training_bar);
            ClipDrawable trainingfill = (ClipDrawable) temp2.getDrawable();
            //max fill is 10000, or tapcount of 5
            trainingtapcount = trainingtapcount + 1;
            trainingfill.setLevel(trainingtapcount*2000);
            startTrain(trainView);
        });
    }

    /**
     * our animation for the battle performed
     */
    public void performbattle(final ArrayList<Integer> rounds, final int enemyattackvalue, boolean bossbattle){
        //create popup display for monster stats durring battle
        LayoutInflater battleinflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert battleinflater != null;
        battleView = battleinflater.inflate(R.layout.battle_info, findViewById(R.id.parent),false);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        //boolean focusable2 = true; // lets taps outside the popup also dismiss it
        final PopupWindow battleWindow = new PopupWindow(battleView, width2, height2);
        battleWindow.setOutsideTouchable(false);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            battleWindow.setElevation(20);
        }
        // show the battle popup window
        // which view you pass in doesn't matter, it is only used for the window token
        battleWindow.setAnimationStyle(R.style.PopupAnimation);
        battleWindow.showAtLocation(findViewById(R.id.placeholder), Gravity.CENTER, 0, 0);
        final TextView enemyhealthtext = battleView.findViewById(R.id.enemy_health_text);
        final TextView playerhealthtext = battleView.findViewById(R.id.player_health_text);
        String temphealth1 = getText(R.string.BattleEnemyHealth)+String.valueOf(enemyhealth);
        enemyhealthtext.setText(temphealth1);
        String temphealth2 = getText(R.string.BattlePlayerHealth)+String.valueOf(currentmonster.getCurrenthealth());
        playerhealthtext.setText(temphealth2);
        ImageView enemyhealthbar = battleView.findViewById(R.id.enemy_health);
        ImageView playerhealthbar = battleView.findViewById(R.id.player_health);
        final ClipDrawable enemyhealthfill = (ClipDrawable) enemyhealthbar.getDrawable();
        //max fill is 10000, or tapcount of 5
        enemyhealthfill.setLevel((int)(10000*(enemyhealth/(float)enemymaxhealth)));
        final ClipDrawable playerhealthfill = (ClipDrawable) playerhealthbar.getDrawable();
        //max fill is 10000, or tapcount of 5
        playerhealthfill.setLevel((int)(10000*(currentmonster.getCurrenthealth()/(float)currentmonster.getMaxhealth())));

        //show our animation for each attack
        final FrameLayout frmlayout = findViewById(R.id.placeholder);
        LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        assert aboutinflater != null;
        final View battle = aboutinflater.inflate(R.layout.battlescreen, null);
        Fade mFade = new Fade(Fade.IN);
        TransitionManager.beginDelayedTransition(frmlayout, mFade);

        frmlayout.removeAllViews();
        frmlayout.addView(battle,0);
        TypedArray maparray = getBaseContext().getResources().obtainTypedArray(currentstoryarray);
        @StyleableRes int index = 4;
        int resource = maparray.getResourceId(index,R.drawable.basic_background);
        maparray.recycle();
        findViewById(R.id.back_screen).setBackgroundResource(resource);

        final ImageView monster = battle.findViewById(R.id.monster_icon);
        final ImageView attack1View = battle.findViewById(R.id.myattack);
        final ImageView attack2View = battle.findViewById(R.id.theirattack);
        switch(enemyattackvalue){
            case 2:
                attack2View.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.attack_2));
                break;
            case 3:
                attack2View.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.attack_3));
                break;
        }

        switch (currentmonster.getPower()){
            case 2:
                attack1View.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.attack_2));
                break;
            case 3:
                attack1View.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.attack_3));
                break;
        }
        //add our different animations together to play
        AnimatorSet battleAnimation = new AnimatorSet();
                ArrayList<Animator> attackanimations = new ArrayList<>();
                //@StyleableRes int index = 4;
                //use our r.array id to find array for current monster
                TypedArray array1 = getApplicationContext().getResources().obtainTypedArray(currentmonster.getArrayid());
                TypedArray array2 = getApplicationContext().getResources().obtainTypedArray(enemyarrayid);
                int[] playerrresources = getResources().getIntArray(currentmonster.getArrayid());
                int[] enemyresources = getResources().getIntArray(enemyarrayid);
                final int player1 = array1.getResourceId(index,R.drawable.egg_idle);
                final int player2 = array2.getResourceId(index,R.drawable.egg_idle);
                final int playerattack1 = array1.getResourceId(index + playerrresources[1]+9,R.drawable.egg_idle);
                final int enemyattack1 = array2.getResourceId(index + enemyresources[1]+9,R.drawable.egg_idle);
                array1.recycle();
                array2.recycle();
                boolean damagedanimation = false;

                for(Integer round : rounds){

                    ValueAnimator playerattack = ValueAnimator.ofFloat(0.0f,1.0f);
                    playerattack.setInterpolator(new LinearInterpolator());
                    playerattack.setDuration(1000L);

                    ValueAnimator player2attack = ValueAnimator.ofFloat(0.0f,1.0f);
                    player2attack.setInterpolator(new LinearInterpolator());
                    player2attack.setDuration(1000L);

                    //the fireballs attacking
                    ValueAnimator attackanimator = ValueAnimator.ofFloat(0.0f, 1.0f);
                    attackanimator.setInterpolator(new LinearInterpolator());
                    attackanimator.setDuration(1300L);

                    ValueAnimator damaged = ValueAnimator.ofFloat(0.0f,1.0f);
                    damaged.setInterpolator(new LinearInterpolator());
                    damaged.setDuration(1000L);

                    switch(round){
                        case 0: //draw
                            attackanimator.setDuration(650L);
                            playerattack.addUpdateListener(animation -> {
                                monster.setBackgroundResource(0);
                                AnimationDrawable tempanimator = (AnimationDrawable) monster.getBackground();
                                if(tempanimator != null){
                                    tempanimator.stop();
                                }
                                monster.setVisibility(View.VISIBLE);
                                monster.setScaleX(1);
                                final float progress = (float) animation.getAnimatedValue();

                                if(progress > 0.7f){
                                    attack1View.setVisibility(View.VISIBLE);
                                    float width =-(progress)* battle.getWidth();
                                    attack1View.setTranslationX(width);
                                    monster.setImageDrawable(ContextCompat.getDrawable(this, playerattack1));
                                }
                                else{
                                    monster.setImageDrawable(ContextCompat.getDrawable(this, player1));
                                    attack1View.setVisibility(View.INVISIBLE);
                                    attack2View.setVisibility(View.INVISIBLE);
                                }
                            });
                            player2attack.addUpdateListener(animation -> {
                                monster.setVisibility(View.VISIBLE);
                                monster.setScaleX(-1);
                                final float progress = (float) animation.getAnimatedValue();
                                if(progress > 0.7f){
                                    attack2View.setVisibility(View.VISIBLE);
                                    float width =(progress)* battle.getWidth();
                                    attack2View.setTranslationX(width);
                                    monster.setImageDrawable(ContextCompat.getDrawable(this, enemyattack1));
                                }
                                else{
                                    monster.setImageDrawable(ContextCompat.getDrawable(this,player2));
                                    attack1View.setVisibility(View.INVISIBLE);
                                    attack2View.setVisibility(View.INVISIBLE);
                                }
                            });
                            attackanimator.addUpdateListener(animation -> {
                                monster.setVisibility(View.INVISIBLE);
                                if(attack2View.getTranslationX() >=  (battle.getWidth()*0.5f - attack2View.getWidth())){
                                    attack1View.setVisibility(View.INVISIBLE);
                                    attack2View.setVisibility(View.INVISIBLE);
                                }
                                else{
                                    attack1View.setVisibility(View.VISIBLE);
                                    attack2View.setVisibility(View.VISIBLE);
                                }
                                final float progress = (float) animation.getAnimatedValue();
                                float width = battle.getWidth()*progress;
                                attack1View.setTranslationX(-width*0.5f);
                                attack2View.setTranslationX(width*0.5f);
                            });

                            break;
                        case 1: //p1 win
                            playerattack.addUpdateListener(animation -> {
                                monster.setBackgroundResource(0);
                                AnimationDrawable tempanimator = (AnimationDrawable) monster.getBackground();
                                if(tempanimator != null){
                                    tempanimator.stop();
                                }
                                monster.setVisibility(View.VISIBLE);
                                monster.setScaleX(1);
                                final float progress = (float) animation.getAnimatedValue();
                                if(progress > 0.7f){
                                    attack1View.setVisibility(View.VISIBLE);
                                    float width =-(progress)* battle.getWidth();
                                    attack1View.setTranslationX(width);
                                    monster.setImageDrawable(ContextCompat.getDrawable(this, playerattack1));
                                }
                                else{
                                    monster.setImageDrawable(ContextCompat.getDrawable(this, player1));
                                    attack1View.setVisibility(View.INVISIBLE);
                                    attack2View.setVisibility(View.INVISIBLE);
                                }
                            });
                            player2attack.addUpdateListener(animation -> {
                                monster.setVisibility(View.VISIBLE);
                                monster.setScaleX(-1);
                                monster.setImageDrawable(ContextCompat.getDrawable(this, player2));
                                attack1View.setVisibility(View.INVISIBLE);
                                attack2View.setVisibility(View.INVISIBLE);
                            });
                            attackanimator.addUpdateListener(animation -> {
                                monster.setVisibility(View.INVISIBLE);
                                attack1View.setVisibility(View.VISIBLE);
                                attack2View.setVisibility(View.INVISIBLE);
                                final float progress = (float) animation.getAnimatedValue();
                                float width = battle.getWidth()*progress;
                                attack1View.setTranslationX(-width);
                                attack2View.setTranslationX(width);
                            });
                            damaged.addUpdateListener(animation -> {
                                monster.setVisibility(View.VISIBLE);
                                monster.setScaleX(-1);
                                final float progress = (float) animation.getAnimatedValue();

                                if(progress < 0.4f){
                                    monster.setImageDrawable(ContextCompat.getDrawable(this, player2));
                                    attack1View.setVisibility(View.VISIBLE);
                                    float width =-progress* battle.getWidth();
                                    attack1View.setTranslationX(width);
                                }
                                else{
                                    monster.setImageResource(0);
                                    monster.setBackgroundResource(R.drawable.damaged);
                                    AnimationDrawable tempanimator = (AnimationDrawable) monster.getBackground();
//                                    monster.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.damaged));
//                                    AnimationDrawable tempanimator = (AnimationDrawable) monster.getDrawable();
                                    tempanimator.start();
                                    attack1View.setVisibility(View.INVISIBLE);
                                    attack2View.setVisibility(View.INVISIBLE);
                                }
                            });
                            damaged.addListener(new AnimatorListenerAdapter()
                            {
                                @Override
                                public void onAnimationEnd(Animator animation)
                                {
                                    enemyhealth = enemyhealth - currentmonster.getPower();
                                    String temphealth = getText(R.string.BattleEnemyHealth)+String.valueOf(enemyhealth);
                                    enemyhealthtext.setText(temphealth);
                                    enemyhealthfill.setLevel((int)((10000L*enemyhealth)/enemymaxhealth));
                                }
                            });
                            damagedanimation = true;
                            break;
                        case 2: //p2 win
                            playerattack.addUpdateListener(animation -> {
                                monster.setBackgroundResource(0);
                                AnimationDrawable tempanimator = (AnimationDrawable) monster.getBackground();
                                if(tempanimator != null){
                                    tempanimator.stop();
                                }
                                monster.setVisibility(View.VISIBLE);
                                monster.setScaleX(1);
                                monster.setImageDrawable(ContextCompat.getDrawable(this, player1));
                                attack1View.setVisibility(View.INVISIBLE);
                                attack2View.setVisibility(View.INVISIBLE);
                            });
                            player2attack.addUpdateListener(animation -> {
                                monster.setVisibility(View.VISIBLE);
                                monster.setScaleX(-1);
                                final float progress = (float) animation.getAnimatedValue();
                                if(progress > 0.7f){
                                    attack2View.setVisibility(View.VISIBLE);
                                    float width =(progress)* battle.getWidth();
                                    attack2View.setTranslationX(width);
                                    monster.setImageDrawable(ContextCompat.getDrawable(this, enemyattack1));
                                }
                                else{
                                    monster.setImageDrawable(ContextCompat.getDrawable(this, player2));
                                    attack1View.setVisibility(View.INVISIBLE);
                                    attack2View.setVisibility(View.INVISIBLE);
                                }
                            });
                            attackanimator.addUpdateListener(animation -> {
                                monster.setVisibility(View.INVISIBLE);
                                attack1View.setVisibility(View.INVISIBLE);
                                attack2View.setVisibility(View.VISIBLE);
                                final float progress = (float) animation.getAnimatedValue();
                                float width = battle.getWidth()*progress;
                                attack1View.setTranslationX(-width);
                                attack2View.setTranslationX(width);
                            });
                            damaged.addUpdateListener(animation -> {
                                monster.setVisibility(View.VISIBLE);
                                monster.setScaleX(1);
                                final float progress = (float) animation.getAnimatedValue();

                                if(progress < 0.4f){
                                    monster.setImageDrawable(ContextCompat.getDrawable(this, player1));
                                    attack2View.setVisibility(View.VISIBLE);
                                    float width =progress* battle.getWidth();
                                    attack2View.setTranslationX(width);
                                }
                                else{
                                    monster.setImageResource(0);
                                    monster.setBackgroundResource(R.drawable.damaged);
                                    AnimationDrawable tempanimator = (AnimationDrawable) monster.getBackground();
//                                    monster.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.damaged));
//                                    AnimationDrawable tempanimator = (AnimationDrawable) monster.getDrawable();
                                    tempanimator.start();
                                    attack1View.setVisibility(View.INVISIBLE);
                                    attack2View.setVisibility(View.INVISIBLE);
                                }
                            });
                            damaged.addListener(new AnimatorListenerAdapter()
                            {
                                @Override
                                public void onAnimationEnd(Animator animation)
                                {
                                    currentmonster.setCurrenthealth(currentmonster.getCurrenthealth()-enemyattackvalue);
                                    String temphealth = getText(R.string.BattlePlayerHealth)+String.valueOf(currentmonster.getCurrenthealth());
                                    playerhealthtext.setText(temphealth);
                                    playerhealthfill.setLevel((int)(10000L*currentmonster.getCurrenthealth())/currentmonster.getMaxhealth());
                                }
                            });
                            damagedanimation = true;
                            break;
                    }

                    attackanimations.add(playerattack);
                    attackanimations.add(player2attack);
                    attackanimations.add(attackanimator);
                    if(damagedanimation)attackanimations.add(damaged);

                }
                battleAnimation.playSequentially(attackanimations);
                Activity mainActivity = this;
                battleAnimation.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        //check if the battle has ended
                        AnimationDrawable tempanimator = (AnimationDrawable) monster.getBackground();
                        if(tempanimator != null){
                            tempanimator.stop();
                        }
                        monster.setBackgroundResource(0);
                        monster.setVisibility(View.INVISIBLE);
                        battleWindow.dismiss();
                        if(enemyhealth > 0 && currentmonster.getCurrenthealth() > 0){
                            monster.setScaleX(1);
                            monster.setVisibility(View.VISIBLE);
                            selectedIcon(currentmonster.getArrayid(), mainActivity);
                            prepareBattle(false, bossbattle);
                        }else{
                            frmlayout.removeAllViews();
                            LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                            assert aboutinflater != null;
                            final View home = aboutinflater.inflate(R.layout.home_screen, null);
                            Fade mFade = new Fade(Fade.IN);
                            TransitionManager.beginDelayedTransition(frmlayout, mFade);

                            frmlayout.addView(home);
                            home.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    selectedIcon(currentarrayid, mainActivity);
                                    Handler h = new Handler();
                                    if(enemyhealth > 0){
                                        attackanimation(mainActivity);
                                    }else {
                                        happyanimation(mainActivity);
                                    }
                                    h.postDelayed(() ->{
                                        selectedIcon(currentarrayid, mainActivity);
                                    }, 3000);
                                    home.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }
                            });
                            isbattling = false;
                            AsyncTask.execute(() -> {
                                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                                Journey temp = db.journeyDao().getJourney().get(0);
                                Monster tempmonster = db.journeyDao().getMonster().get(0);
                                temp.setIsbattling(false);
                                temp.setBossfight(false);
                                Random ran = new Random();
                                temp.setEventsteps(ran.nextInt(nexteventran) + 500);
                                if(ran.nextFloat() < 0.8){
                                    temp.setEventtype(2);
                                }
                                else{
                                    temp.setEventtype(1);
                                }
                                temp.setEventreached(false);
                                if(enemyhealth > 0 ){
                                    tempmonster.battleLost();
                                    temp.setStorysteps(ran.nextInt(nexteventran) + 1000 + temp.getStorysteps());
                                }
                                db.journeyDao().updateMonster(tempmonster);
                                db.journeyDao().update(temp);
                            });
                            if(bossbattle && currentmonster.getCurrenthealth() > 0){
                                BossDefeated runner = new BossDefeated(mainActivity);
                                runner.execute();
                            }
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                //play our animation for this battle
                battleAnimation.start();
    }

    /**
     * fetch the Uri of the media
     * @param mediaName name of the media
     * @return Uri of the media
     */
    private Uri getMedia(String mediaName) {
        if (URLUtil.isValidUrl(mediaName)) {
            // media name is an external URL
            return Uri.parse(mediaName);
        } else { // media name is a raw resource embedded in the app
            return Uri.parse("android.resource://" + getPackageName() +
                    "/raw/" + mediaName);
        }
    }

    /**
     * start our video
     */
    public void startVideo(View v){
//        LayoutInflater confirminflater = (LayoutInflater)
//                getSystemService(LAYOUT_INFLATER_SERVICE);
//        assert confirminflater != null;
//        View videoView = confirminflater.inflate(R.layout.video_view,findViewById(R.id.parent), false);
//        int width2 = FrameLayout.LayoutParams.MATCH_PARENT;
//        int height2 = FrameLayout.LayoutParams.MATCH_PARENT;
//        final PopupWindow confirmWindow = new PopupWindow(videoView, width2, height2, true);
//        confirmWindow.showAtLocation(findViewById(R.id.placeholder), Gravity.CENTER, 0, 0);
//
//        confirmWindow.setOnDismissListener(()->{
//            if(v == null){
//                aboutPopup();
//            }
//        });
//
//        VideoView mVideoView = videoView.findViewById(R.id.videoview);
//        Toast.makeText(this,"prepared", Toast.LENGTH_SHORT).show();
//        Uri videoUri = getMedia(VIDEO_SAMPLE);
//        mVideoView.setVideoURI(videoUri);
//
//        mVideoView.setOnCompletionListener(mediaPlayer -> {
//            confirmWindow.dismiss();
//            if(!isplaying){
//                music.start();
//            }
//        });
//        mVideoView.setOnPreparedListener(mediaPlayer -> {
//            if(!isplaying){
//                music.pause();
//            }
//            else{
//                mediaPlayer.setVolume(0f,0f);
//            }
//            mVideoView.start();
//
//        });
//        mVideoView.setOnClickListener((view) -> {
//            confirmWindow.dismiss();
//            if(!isplaying){
//                music.start();
//            }
//        });
//        mVideoView.requestFocus();
        music.release();
        offlineService = true;
        Intent intent = new Intent(this, VideoPlayer.class);
        startActivity(intent);
        AsyncTask.execute(()->{
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            Journey temp = db.journeyDao().getJourney().get(0);
            temp.setShowabout(true);
            db.journeyDao().update(temp);
        });
    }

    //resume our activity
    static private class AsyncResume extends AsyncTask<String,TextView,String>{
        // Weak references will still allow the Activity to be garbage-collected
        private final WeakReference<Activity> weakActivity;

        public AsyncResume(Activity myActivity){
            this.weakActivity = new WeakReference<>(myActivity);
        }
        private int eventtype;
        private long storysteps, eventsteps;
        private long matchsteps;
        private String message;
        private String monstername;
        private boolean isbattling, isboss, ismatching;
        private int enemyarrayid, enemyhealth, enemymaxhealth;
        private boolean firsttime;
        private boolean aboutshowed;
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.getInstance(weakActivity.get());
            String event = (String)weakActivity.get().getText(R.string.stepsneeded);
            Journey temp = db.journeyDao().getJourney().get(0);
            monstername = db.journeyDao().getMonster().get(0).getName();
            long stepsneeded;
            if(db.journeyDao().getMonster().get(0).getHatched()){
                stepsneeded = temp.getStorysteps();
            }else{
                stepsneeded = temp.getEventsteps();
            }
            boolean isEventreached = temp.isEventreached();
            eventsteps = temp.getEventsteps();
            message = event + stepsneeded;
            eventtype = temp.getEventtype();
            storysteps = temp.getStorysteps();
            isboss = temp.isBossfight();
            MainActivity mainActivity = (MainActivity) weakActivity.get();
            mainActivity.currentarrayid = db.journeyDao().getMonster().get(0).getArrayid();
            matchsteps = temp.getMatchmakersteps();
            ismatching = temp.isMatching();
//            if(temp.isMatching()){
//                matchsteps = temp.getMatchmakersteps();
//            }
            aboutshowed = temp.isShowabout();
            isbattling = temp.getIsbattling();
            if(isbattling){
                enemyarrayid = temp.getEnemyarrayid();
                enemyhealth = temp.getEnemyhealth();
                enemymaxhealth = temp.getEnemymaxhealth();
            }
            firsttime = db.journeyDao().getJourney().get(0).isFirsttime();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if(!firsttime){
                MainActivity mainActivity = (MainActivity) weakActivity.get();
                if(!aboutshowed){
                    mainActivity.aboutPopup();
                    mainActivity.startVideo(null);
                }
                TextView TvSteps = weakActivity.get().findViewById(R.id.tv_steps);
                //TextView MonsterName = weakActivity.get().findViewById(R.id.monster_name);
                TvSteps.setText(message);
                //MonsterName.setText(monstername);

                if(isbattling){
                    mainActivity.enemymaxhealth = enemymaxhealth;
                    mainActivity.enemyarrayid = enemyarrayid;
                    mainActivity.enemyhealth = enemyhealth;
                    mainActivity.prepareBattle(false, isboss);
                }
                else if(storysteps <= 0){
                    //event 4 is for when boss is found
                    mainActivity.startEvent(4,weakActivity.get());
                }
                else if(eventsteps <= 0) {
                    mainActivity.startEvent(eventtype,weakActivity.get());
                }
                else if(matchsteps <= 0 && ismatching){
                    mainActivity.startEvent(3,weakActivity.get());
                }
            }

        }
    }

    //fetch and display the monster info for current status
    private class MonsterInfo extends AsyncTask<String,TextView,String>{

        private int hunger;
        private int training;
        private String name;
        private int arrayid;
        long evolvesteps;
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            Monster temp = db.journeyDao().getMonster().get(0);
            hunger = temp.getHunger();
            training = temp.getDiligence();
            name = temp.getName();
            arrayid = temp.getArrayid();
            evolvesteps = temp.getEvolvesteps();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            //where max clip is 10000, hunger/diligence is in increments of 8
            ImageView temp = aboutView.findViewById(R.id.hungerfill);
            ClipDrawable hungerfill = (ClipDrawable) temp.getDrawable();
            ImageView temp2 = aboutView.findViewById(R.id.trainingfill);

            ClipDrawable trainingfill = (ClipDrawable) temp2.getDrawable();
            hungerfill.setLevel(hunger*1250);
            trainingfill.setLevel(training*1250);

            EditText nameView = aboutView.findViewById(R.id.monster_popup_name);
            nameView.setText(name);
            nameView.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    AsyncTask.execute(()->{
                        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                        Monster temp1 = db.journeyDao().getMonster().get(0);
                        temp1.setName(nameView.getText().toString());
                        db.journeyDao().updateMonster(temp1);
                    });
                    nameView.setCursorVisible(false);
                    return false;
                }
                else if(event.getKeyCode() == KeyEvent.KEYCODE_BACK){
                    nameView.setCursorVisible(false);
                    return false;
                }
                return false;
            });
            nameView.setOnClickListener(v -> nameView.setCursorVisible(true));


            @StyleableRes int index = 4;
            TypedArray array = getApplicationContext().getResources().obtainTypedArray(arrayid);
            int resource = array.getResourceId(index,R.drawable.egg_idle);
            int[] monsterresources = getResources().getIntArray(arrayid);
            array.recycle();
            ImageView infoView = aboutView.findViewById(R.id.monster_popup_icon);
            infoView.setBackgroundResource(resource);

            AnimationDrawable infoanimator = (AnimationDrawable) infoView.getBackground();
            infoanimator.start();

            TextView evolvetext = aboutView.findViewById(R.id.stepsevolvenumber);
            if(monsterresources[0] == 3){
                evolvetext.setText(getText(R.string.maxstage));
                aboutView.findViewById(R.id.stepsevolve).setVisibility(View.INVISIBLE);
            }
            else if(evolvesteps <= 0){
                evolvetext.setText(getText(R.string.readytoevolve));
                aboutView.findViewById(R.id.stepsevolve).setVisibility(View.INVISIBLE);
            }else{
                evolvetext.setText(String.valueOf(evolvesteps));
            }
        }
    }

    static private class HatchedInfo extends AsyncTask<String,TextView,String> {
        private int arrayid;
        //private View viewid;
        // Weak references will still allow the Activity to be garbage-collected
        private final WeakReference<Activity> weakActivity;

        public HatchedInfo(Activity myActivity){
            this.weakActivity = new WeakReference<>(myActivity);
            //this.viewid = viewid;
        }

        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.getInstance(weakActivity.get());
            Monster temp = db.journeyDao().getMonster().get(0);
            arrayid = temp.getArrayid();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            @StyleableRes int index = 4;
            MainActivity activity = (MainActivity) weakActivity.get();
            TypedArray array = weakActivity.get().getApplicationContext().getResources().obtainTypedArray(arrayid);
            int resource = array.getResourceId(index,R.drawable.egg_idle);
            array.recycle();
            ImageView infoView = activity.hatchedView.findViewById(R.id.monster_popup_icon);
            //ImageView infoView = viewid.findViewById(R.id.monster_popup_icon);
            infoView.setBackgroundResource(resource);
            AnimationDrawable infoanimator = (AnimationDrawable) infoView.getBackground();
            infoanimator.start();
        }
    }

    /**
     * AsyncTask to check if monster is hatched before starting selected activity
     */
    private static class StartCheck extends AsyncTask<String,TextView,String> {
        private boolean hatched;
        private int toStart;
        private final WeakReference<Activity> weakActivity;

        public StartCheck(Activity myActivity, int toStart){
            this.weakActivity = new WeakReference<>(myActivity);
            this.toStart = toStart;
        }
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.getInstance(weakActivity.get());
            Monster temp = db.journeyDao().getMonster().get(0);
            hatched = temp.getHatched();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if(hatched){
                MainActivity mainActivity = (MainActivity) weakActivity.get();
                mainActivity.music.release();
                mainActivity.offlineService = true;
                Intent intent;
                switch(toStart){
                    case 1:
                        intent = new Intent(weakActivity.get(), Training.class);
                        break;
                    case 2:
                        intent = new Intent(weakActivity.get(), Communication.class);
                        break;
                    default:
                        intent = new Intent(weakActivity.get(), Ranch.class);
                        break;
                }
                weakActivity.get().startActivity(intent);
                //where right side is current view
                weakActivity.get().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
            else {
                Toast.makeText(weakActivity.get(), "Hatch your monster first.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * check if this is the first time app has been initialized
     */
    static private class FirstTimeCheck extends AsyncTask<String,TextView,String> {
        private boolean firsttime;
        // Weak references will still allow the Activity to be garbage-collected
        private final WeakReference<Activity> weakActivity;

        public FirstTimeCheck(Activity myActivity){
            this.weakActivity = new WeakReference<>(myActivity);
        }
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.getInstance(weakActivity.get());
            //check if database has been set up, if not then populate it
            if(db.journeyDao().getJourney().size() == 0){
                db.journeyDao().insertAll(Journey.populateData());
                db.journeyDao().insertMonster(Monster.populateData());
                db.journeyDao().insertUnlockedMonster(UnlockedMonster.populateData());
                db.journeyDao().insertCompletedMaps(CompletedMaps.populateData());
            }
            firsttime = db.journeyDao().getJourney().get(0).isFirsttime();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            MainActivity activity = (MainActivity) weakActivity.get();
            if (firsttime) {
                activity.eggSelect();
            }
        }
    }

    /**
     * AsyncTask to start the monster
     */
    static private class DisplayMonster extends AsyncTask<String,TextView,String> {
        private int arrayid;
        private boolean firsttime;
        // Weak references will still allow the Activity to be garbage-collected
        private final WeakReference<Activity> weakActivity;

        public DisplayMonster(Activity myActivity){
            this.weakActivity = new WeakReference<>(myActivity);
        }
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.getInstance(weakActivity.get());
            arrayid = db.journeyDao().getMonster().get(0).getArrayid();
            firsttime = db.journeyDao().getJourney().get(0).isFirsttime();

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            MainActivity activity = (MainActivity) weakActivity.get();
            if(!firsttime){
                activity.selectedIcon(arrayid,weakActivity.get());
            }
        }
    }

    /**
     * AsyncTask to finish boss battle
     */
    static private class BossDefeated extends AsyncTask<String,TextView,String> {
        // Weak references will still allow the Activity to be garbage-collected
        private final WeakReference<Activity> weakActivity;
        private int awardtype;
        private int rewardedegg;
        List<CompletedMaps> basicMapList;

        public BossDefeated(Activity myActivity){
            this.weakActivity = new WeakReference<>(myActivity);
        }
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.getInstance(weakActivity.get());
            awardtype = R.string.evolutionitem;
            boolean completedbefore = false;
            List<CompletedMaps> completedMapsList = db.journeyDao().getCompletedMaps();
            basicMapList = new ArrayList<>();
            int completedbasic = 6;
            List<CompletedMaps> advancedMapList = new ArrayList<>();
            List<CompletedMaps> cosmicMapList = new ArrayList<>();
            Journey temp = db.journeyDao().getJourney().get(0);
            long currentdiscount = temp.getEvolveddiscount();
            for (CompletedMaps completedMaps : completedMapsList){
                if(completedMaps.getMaparray() == temp.getStorytype()){
                    //completedMap = completedMaps;
                    if(completedMaps.isStorycompleted()){
                        db.journeyDao().insertItem(new Item(4));
                        rewardedegg = R.drawable.ic_food_display_4;
                        completedbefore = true;
                    }
                    else{
                        completedMaps.setStorycompleted(true);
                    }
                    temp.setStorysteps(completedMaps.getStorysteps());
                    //break;
                }
                if(completedMaps.isIsbasic() == 0 && !completedMaps.isStorycompleted()){
                    completedbasic--;
                    if(!completedMaps.isIsstarter()){
                        basicMapList.add(completedMaps);
                    }
                }
                else if(completedMaps.isIsbasic() == 1 && !completedMaps.isIsunlocked()){
                    advancedMapList.add(completedMaps);
                }
                else if(completedMaps.isIsbasic() == 2 && !completedMaps.isIsunlocked()){
                    cosmicMapList.add(completedMaps);
                }
            }
            if(basicMapList.size() > 0  && !completedbefore){
                UnlockEgg(db, completedMapsList, basicMapList);
            }
            else if(advancedMapList.size() > 0 && !completedbefore && completedbasic >=6){
                UnlockEgg(db,completedMapsList,advancedMapList);
            }
            else if(cosmicMapList.size() > 0 && !completedbefore && advancedMapList.size() <= 0 && completedbasic >=6){
                UnlockEgg(db,completedMapsList, cosmicMapList);
            }
            else if(!completedbefore){
                if(currentdiscount < 4000){
                    rewardedegg = R.drawable.ic_costdown;
                    awardtype = R.string.evolutiondiscount;
                    temp.setEvolvediscount(temp.getEvolveddiscount()+2000);
                }
                else {
                    rewardedegg = R.drawable.ic_food_display_4x3;
                    db.journeyDao().insertItem(new Item(4));
                    db.journeyDao().insertItem(new Item(4));
                    db.journeyDao().insertItem(new Item(4));
                    awardtype = R.string.evolutionitem3;
                }

            }
            db.journeyDao().update(temp);
            db.journeyDao().updateCompletedMaps(completedMapsList);
            return null;
        }

        /**
         *Unlocks a new egg for us to use
         * @param db the database we are saving to
         * @param completedMapsList the list of completed maps
         */
        private void UnlockEgg(AppDatabase db, List<CompletedMaps> completedMapsList, List<CompletedMaps> basicMapList){
            Random ran = new Random();
            @StyleableRes int index = 4;
            CompletedMaps mapadded = basicMapList.get(ran.nextInt(basicMapList.size()));
            TypedArray array = weakActivity.get().getBaseContext().getResources().obtainTypedArray(mapadded.getMaparray());
            int unlockedegg = array.getResourceId(index+3, R.array.enigma_egg);
            TypedArray eggarray = weakActivity.get().getBaseContext().getResources().obtainTypedArray(unlockedegg);
            rewardedegg = eggarray.getResourceId(index,R.drawable.egg_idle);
            boolean alreadyunlocked = false;
            List<UnlockedMonster> unlockedMonsters = db.journeyDao().getUnlockedMonster();
            for(UnlockedMonster unlockedMonster: unlockedMonsters){
                if(unlockedMonster.getMonsterarrayid() == unlockedegg){
                    alreadyunlocked = unlockedMonster.isUnlocked();
                    unlockedMonster.setDiscovered(true);
                    unlockedMonster.setUnlocked(true);
                    break;
                }
            }
            for(CompletedMaps completedMaps: completedMapsList){
                if(completedMaps.equals(mapadded)){
                    completedMaps.setIsstarter(true);
                    completedMaps.setIsunlocked(true);
                    break;
                }
            }
            if(alreadyunlocked){
                //give 3 evolution items if the egg has already been bought
                db.journeyDao().insertItem(new Item(4));
                db.journeyDao().insertItem(new Item(4));
                db.journeyDao().insertItem(new Item(4));
            }
            db.journeyDao().updateUnlockedMonster(unlockedMonsters);

            array.recycle();
            eggarray.recycle();
            awardtype = R.string.newegg;
        }

        @Override
        protected void onPostExecute(String result) {
            MainActivity activity = (MainActivity) weakActivity.get();
            activity.bossDefeatPopup(awardtype, rewardedegg);
        }
    }

    /**
     * save volume settings to shared pref on our app
     * @param volume the string to save our value to
     * @param value the level of volume
     */
    public void saveVolume(String volume, int value){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME,0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(volume,value);
        editor.apply();
    }

    /**
     * override the back pressed to bring up the option to go back
     */
    @Override
    public void onBackPressed() {
        LayoutInflater confirminflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert confirminflater != null;
        View confirmView = confirminflater.inflate(R.layout.confirm_popup,findViewById(R.id.parent), false);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow confirmWindow = new PopupWindow(confirmView, width2, height2, true);
        confirmWindow.setOutsideTouchable(false);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            confirmWindow.setElevation(20);
        }
        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        confirmWindow.setAnimationStyle(R.style.PopupAnimation);
        confirmWindow.showAtLocation(findViewById(R.id.placeholder), Gravity.CENTER, 0, 0);
        confirmView.findViewById(R.id.close).setOnClickListener(v -> confirmWindow.dismiss());

        confirmView.findViewById(R.id.back).setOnClickListener(v -> confirmWindow.dismiss());

        confirmView.findViewById(R.id.confirm).setOnClickListener(v ->{
            confirmWindow.dismiss();
            super.onBackPressed();
        } );

        TextView message = confirmView.findViewById(R.id.confimation_text);
        message.setText(getText(R.string.quit));
    }

    /**
     * AsyncTask to start the care
     */
    static private class StorePopup extends AsyncTask<String,TextView,String> {
        // Weak references will still allow the Activity to be garbage-collected
        private final WeakReference<Activity> weakActivity;

        public StorePopup(Activity myActivity){
            this.weakActivity = new WeakReference<>(myActivity);
        }
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.getInstance(weakActivity.get());
            MainActivity activity = (MainActivity) weakActivity.get();

            List<CompletedMaps> completedMapsList = db.journeyDao().getCompletedMaps();
            for(CompletedMaps completedMaps : completedMapsList){
                if(completedMaps.getMaparray() == R.array.dark_egg && completedMaps.isIsunlocked()){
                    SharedPreferences.Editor editor2 = activity.settings.edit();
                    editor2.putBoolean("darkisbought", true);
                    editor2.apply();
                }
                else if(completedMaps.getMaparray() == R.array.light_egg && completedMaps.isIsunlocked()){
                    SharedPreferences.Editor editor2 = activity.settings.edit();
                    editor2.putBoolean("lightisbought", true);
                    editor2.apply();
                }
                else if(completedMaps.getMaparray() == R.array.cosmic_egg && completedMaps.isIsunlocked()){
                    SharedPreferences.Editor editor2 = activity.settings.edit();
                    editor2.putBoolean("cosmicisbought", true);
                    editor2.apply();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            MainActivity activity = (MainActivity) weakActivity.get();
            activity.store();

        }
    }

    /**
     * AsyncTask to validate purchase
     */
    static private class ValidatePurchase extends AsyncTask<String,TextView,String> {
        // Weak references will still allow the Activity to be garbage-collected
        private final WeakReference<Activity> weakActivity;
        private int boughtitem;
        boolean alreadypurchased;

        public ValidatePurchase(Activity myActivity, int boughtitem){
            this.weakActivity = new WeakReference<>(myActivity);
            this.boughtitem = boughtitem;
        }
        @Override
        protected String doInBackground(String... strings) {
            int desiredarray = R.array.dark_egg;
            int desiredmap = R.array.dark_map;
            alreadypurchased = false;
            switch (boughtitem){
                case 0:
                    return null;
                case 1:
                    desiredarray = R.array.dark_egg;
                    desiredmap = R.array.dark_map;
                    break;
                case 2:
                    desiredarray = R.array.light_egg;
                    desiredmap = R.array.light_map;
                    break;
                case 3:
                    desiredarray = R.array.cosmic_egg;
                    desiredmap = R.array.cosmic_map;
                    break;
            }
            AppDatabase db = AppDatabase.getInstance(weakActivity.get());
            List<UnlockedMonster> unlockedMonsters = db.journeyDao().getUnlockedMonster();
            for(UnlockedMonster unlockedMonster : unlockedMonsters){
                if(unlockedMonster.getMonsterarrayid() == desiredarray){
                    unlockedMonster.setUnlocked(true);
                    unlockedMonster.setDiscovered(true);
                    break;
                }
            }
            List<CompletedMaps> completedMapsList = db.journeyDao().getCompletedMaps();
            for(CompletedMaps completedMaps: completedMapsList){
                if(completedMaps.getMaparray() == desiredmap){
                    alreadypurchased = completedMaps.isIsunlocked();
                    completedMaps.setIsstarter(true);
                    completedMaps.setIsunlocked(true);
                    break;
                }
            }
            db.journeyDao().updateUnlockedMonster(unlockedMonsters);
            db.journeyDao().updateCompletedMaps(completedMapsList);

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            MainActivity activity = (MainActivity) weakActivity.get();
            if(alreadypurchased){
                return;
            }
            switch(boughtitem){
                case 0:
                    activity.purchasemadePopup(R.drawable.ic_adbought, R.string.removeads);
                    break;
                case 1:
                    activity.purchasemadePopup(R.drawable.egg_dark_idle, R.string.darkeggbought);
                    break;
                case 2:
                    activity.purchasemadePopup(R.drawable.egg_light_idle, R.string.lighteggbought);
                    break;
                case 3:
                    activity.purchasemadePopup(R.drawable.egg_cosmic_idle, R.string.cosmiceggbought);
                    break;
            }


        }
    }

    /**
     * broadcast receiver for handling events
     */
    public class StepReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            handleEvent();
        }
    }
}
