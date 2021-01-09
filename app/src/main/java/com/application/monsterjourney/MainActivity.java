package com.application.monsterjourney;

import androidx.annotation.StyleableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
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
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "MyJourneyFile";
    SharedPreferences settings;

    private TextView totaltime;
    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    private TextView TvSteps, MonsterName;
    private boolean runbackground; //the check for if we track steps while the app is closed

    private Monster currentmonster;

    public int currentarrayid;
    private int currentstoryarray;

    private NumberPicker picker;
    private Button selectedpick;

    public View aboutView, trainView, battleView, optionsView, storeView, matchView;
    public int trainingtapcount;

    private int enemyarrayid, enemyhealth, enemymaxhealth;

    private AdView mAdView;
    private BillingClient billingClient;

    private MediaPlayer music;
    private int currentvolume;
    private boolean isplaying;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = getSharedPreferences(PREFS_NAME, 0);
        TvSteps = (TextView) findViewById(R.id.tv_steps);
        MonsterName = (TextView) findViewById(R.id.monster_name);
        Button btnStart = (Button) findViewById(R.id.btn_start);
        totaltime = findViewById(R.id.total_time);
        picker = (NumberPicker) findViewById(R.id.picker);
        selectedpick = (Button) findViewById(R.id.selection);

        boolean isbought = settings.getBoolean("isbought", false);
        setupBillingClient();

        //check if player has paid for app to remove ads, if not then initialize and load our ad
        mAdView = findViewById(R.id.adView);
        mAdView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(!isbought){
                    MobileAds.initialize(getApplicationContext(), initializationStatus -> {
                    });

                    AdRequest adRequest = new AdRequest.Builder().build();
                    mAdView.loadAd(adRequest);
                }
                else{
                    mAdView.pause();
                    mAdView.setVisibility(View.GONE);
                }

                String[] pickervals = new String[]{"Library", "Map", "Care", "Minigame","Connect", "Shop"};

                picker.setMaxValue(5);
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

                mAdView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        runbackground = settings.getBoolean("runbackground", true);



        //TODO remove/replace ui for something else since background process is toggled in options now
        btnStart.setOnClickListener(arg0 -> {
//            monster_hatched(this);
            AsyncTask.execute(() -> {
                AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
                List<UnlockedMonster> unlockedMonsters = db.journeyDao().getUnlockedMonster();
                for(UnlockedMonster unlockedMonster : unlockedMonsters){
                    unlockedMonster.setUnlocked(true);
                    unlockedMonster.setDiscovered(true);

                }

                db.journeyDao().updateUnlockedMonster(unlockedMonsters);
//                Journey tempjourney = db.journeyDao().getJourney().get(0);
//                //tempjourney.addStepstoJourney(100);
//                Monster tempmonster = db.journeyDao().getMonster().get(0);
//                tempmonster.setEvolvesteps(0);
//                db.journeyDao().updateMonster(tempmonster);
//                //matchmaking event
//                tempjourney.setEventtype(1);
//                tempjourney.setEventsteps(0);
////                tempjourney.setEventsteps(10);
////                tempjourney.setMatching(true);
////                tempjourney.setMatchmakersteps(0);
//                tempjourney.setEventreached(true);
////                Monster tempmonster = db.journeyDao().getMonster().get(0);
////                //tempmonster.setEvolvesteps(tempmonster.getEvolvesteps() - 100);
//                db.journeyDao().update(tempjourney);
////                db.journeyDao().updateMonster(tempmonster);
            });

        });

        findViewById(R.id.test).setOnClickListener(v -> prepareBattle(true));

        findViewById(R.id.game_options).setOnClickListener(v -> options());

        Service mForeGroundService = new ForeGroundService();
        Intent mServiceIntent = new Intent(this, mForeGroundService.getClass());
        if (!isMyServiceRunning(mForeGroundService.getClass())) {
            startService(mServiceIntent);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        long steps = intent.getLongExtra(ForeGroundService.STEP_COUNT, 0);
                        int eventtype = intent.getIntExtra(ForeGroundService.EVENT_TYPE,0);

                        totaltime.setText( TEXT_NUM_STEPS + steps);

                        handleEvent();
                    }
                }, new IntentFilter(ForeGroundService.ACTION_BROADCAST)
        );

        //add our home screen with the current monster
        final FrameLayout frmlayout = (FrameLayout) findViewById(R.id.placeholder);
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
                        if(aboutView != null){
                            return;
                        }
                        LayoutInflater aboutinflater = (LayoutInflater)
                                getSystemService(LAYOUT_INFLATER_SERVICE);
                        assert aboutinflater != null;
                        aboutView = aboutinflater.inflate(R.layout.monster_popup, null);
                        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
                        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
                        final PopupWindow aboutWindow = new PopupWindow(aboutView, width2, height2, true);


                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            aboutWindow.setElevation(20);
                        }
                        // show the popup window
                        // which view you pass in doesn't matter, it is only used for the window token
                        aboutWindow.setAnimationStyle(R.style.PopupAnimation);
                        //TODO add option to give a name to your monster if it doesn't have a name
                        aboutWindow.showAtLocation(findViewById(R.id.monster_info_popup), Gravity.CENTER, 0, 0);
                        aboutView.findViewById(R.id.close).setOnClickListener(v1 -> {
                            aboutWindow.dismiss();
                            aboutView = null;
                        });
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
        AsyncTask.execute(() -> {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            Journey tempjourney = db.journeyDao().getJourney().get(0);
            totaltime.setText(String.valueOf(tempjourney.getTotalsteps()));

            boolean firsttime = tempjourney.isFirsttime();
            if (firsttime) {
                eggSelect();
            }

            currentmonster = db.journeyDao().getMonster().get(0);
            currentarrayid = currentmonster.getArrayid();
        });


    }

    /**
     * create our options popup for the game, music and stuff
     */
    public void options(){
        if(optionsView != null){
            return;
        }
        LayoutInflater optionsinflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert optionsinflater != null;
        optionsView = optionsinflater.inflate(R.layout.options_popup, null);
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


        SeekBar volControl = (SeekBar)optionsView.findViewById(R.id.volumeBar);
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

        optionsView.findViewById(R.id.ratebutton).setOnClickListener(v -> {
            try{
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
            } catch (ActivityNotFoundException e){
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
            }
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
    protected void onDestroy() {
        //if(!isplaying)music.pause();
        music.release();
        if(runbackground){
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("restartservice");
            broadcastIntent.setClass(this, Restarter.class);
            this.sendBroadcast(broadcastIntent);
        } else {
            stopService();
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!isplaying)music.pause();
        //music.release();

    }

    @Override
    protected void onResume() {
        Activity mainActivity = this;
//        findViewById(R.id.placeholder).getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                //TODO add better implementation of current story array
////                AsyncTask.execute(() -> {
////                    AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
////                    currentarrayid = db.journeyDao().getMonster().get(0).getArrayid();
////                    currentstoryarray = db.journeyDao().getJourney().get(0).getStorytype();
////                    //selectedIcon(currentarrayid, mainActivity);
////                });
////                DisplayMonster runner = new DisplayMonster(mainActivity);
////                runner.execute();
//                //handleEvent();
//                findViewById(R.id.placeholder).getViewTreeObserver().removeOnGlobalLayoutListener(this);
//            }
//        });
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

        if(!isplaying)music.start();

        AsyncTask.execute(() -> {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            currentarrayid = db.journeyDao().getMonster().get(0).getArrayid();
            currentstoryarray = db.journeyDao().getJourney().get(0).getStorytype();
            //selectedIcon(currentarrayid, mainActivity);
        });

        DisplayMonster runner = new DisplayMonster(mainActivity);
        runner.execute();

        handleEvent();

        super.onResume();
    }

    /**
     * start our foreground step tracking service
     *
     * TODO this may not be needed
     */
    public void startService() {
        Intent serviceIntent = new Intent(this, ForeGroundService.class);
        serviceIntent.putExtra("inputExtra", "Foreground Service Example in Android");

        //ContextCompat.startForegroundService(this, serviceIntent);
        startService(serviceIntent);
    }

    /**
     * stop our foreground step tracking service
     */
    public void stopService() {
        Intent serviceIntent = new Intent(this, ForeGroundService.class);
        stopService(serviceIntent);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        //TODO maybe change this?
        //monsteranimator.start();
        super.onWindowFocusChanged(hasFocus);
    }

    /**
     * open the egg select menu
     */
    public void eggSelect(){
        //if(!isplaying)music.pause();
        Intent intent = new Intent(this, EggSelect.class);
        startActivity(intent);
        //where right side is current view
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
        //change views and close current selection, it will be re opened when re selected
    }

    /**
     * open the library view
     */
    public void library(){
        //if(!isplaying)music.pause();
        Intent intent = new Intent(this, Library.class);
        startActivity(intent);
        //where right side is current view
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    /**
     * open the map
     */
    public void map(){
        //if(!isplaying)music.pause();
        Intent intent = new Intent(this, Map.class);
        startActivity(intent);
        //where right side is current view
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    /**
     * open training activity
     */
    public void train(){
        StartTraining runner = new StartTraining();
        runner.execute();
    }

    /**
     * open minigame activity
     */
    public void minigame(){
        //if(!isplaying)music.pause();
        Intent intent = new Intent(this, Minigame.class);
        startActivity(intent);
        //where right side is current view
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    /**
     * open connect activity for communicating with friends
     */
    public void connect(){
        if(!isplaying)music.pause();
        Intent intent = new Intent(MainActivity.this, Communication.class);
        startActivity(intent);
        //where right side is current view
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }


    //TODO make sure that a purchase made is kept once acknowledged
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
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("isbought", true);
                editor.apply();
                if(mAdView != null){
                    mAdView.pause();
                    mAdView.setVisibility(View.GONE);
                }
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    if (!purchase.isAcknowledged()) {
                        AcknowledgePurchaseParams acknowledgePurchaseParams =
                                AcknowledgePurchaseParams.newBuilder()
                                        .setPurchaseToken(purchase.getPurchaseToken())
                                        .build();
                        billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
                    }
                }
                break;
            case "purchase_dark_egg":
                //TODO add the new eggs and logic to unlock them
                AsyncTask.execute(()->{
                    AppDatabase db = AppDatabase.buildDatabase(this);
                    List<UnlockedMonster> unlockedMonsters = db.journeyDao().getUnlockedMonster();
                    for(UnlockedMonster unlockedMonster : unlockedMonsters){
                        if(unlockedMonster.getMonsterarrayid() == R.array.dark_egg){
                            unlockedMonster.setUnlocked(true);
                            unlockedMonster.setDiscovered(true);
                            break;
                        }
                    }
                });
                SharedPreferences.Editor editor2 = settings.edit();
                editor2.putBoolean("darkisbought", true);
                editor2.apply();
                break;
            case "purchase_light_egg":
                SharedPreferences.Editor editor3 = settings.edit();
                editor3.putBoolean("lightisbought", true);
                editor3.apply();
                break;
            case "purchase_cosmic_egg":
                SharedPreferences.Editor editor4 = settings.edit();
                editor4.putBoolean("cosmicisbought", true);
                editor4.apply();
                break;
            case "purchase_item_bundle":
                AsyncTask.execute(() -> {
                    AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
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
                ConsumeParams consumeParams =
                        ConsumeParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();

                ConsumeResponseListener listener = (billingResult, purchaseToken) -> {
//                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
//                        // Handle the success of the consume operation.
//                    }
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

//        List<String> skuList = new ArrayList<> ();
//        skuList.add("remove_advertisements");
//        skuList.add("purchase_light_egg");
//        skuList.add("purchase_dark_egg");
//        skuList.add("purchase_cosmic_egg");
//        skuList.add("purchase_item_bundle");

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
                        //TODO implement proper purchase results after testing
                        // Process the result.
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
                        //TODO implement proper purchase results after testing
                        // Process the result.
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
                        //TODO implement proper purchase results after testing
                        // Process the result.
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
                //TODO implement proper purchase results after testing
                        // Process the result.
                        //can't test purchases on emulator, must be part of an alpha test track
                        assert skuDetailsList != null;
                        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                .setSkuDetails(skuDetailsList.get(0))
                                .build();
                        int responseCode = billingClient.launchBillingFlow(this, billingFlowParams).getResponseCode();
                    });

        });

        if(settings.getBoolean("isbought", false)){
            storeView.findViewById(R.id.buy_ad_container).setVisibility(View.GONE);
        }
        if(settings.getBoolean("darkisbought", false)){
            storeView.findViewById(R.id.buydarkegg).setVisibility(View.GONE);
        }
        if(settings.getBoolean("lightisbought", false)){
            storeView.findViewById(R.id.buylightegg).setVisibility(View.GONE);
        }
        if(settings.getBoolean("cosmicisbought", false)){
            storeView.findViewById(R.id.buycosmicegg).setVisibility(View.GONE);
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
        int resource = array.getResourceId(index,R.drawable.egg_idle);
        ImageView imageView = activity.findViewById(R.id.monster_icon);
        imageView.setImageDrawable(ContextCompat.getDrawable(activity.getApplicationContext(), resource));
        imageView.setBackgroundResource(R.drawable.basic_background);

        AnimationDrawable monsteranimator = (AnimationDrawable) imageView.getDrawable();
        monsteranimator.start();
        array.recycle();
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
        ImageView eventimage = activity.findViewById(R.id.monster_event);
        eventimage.setVisibility(View.VISIBLE);
        final Animation eventanimation = new AlphaAnimation(1, 0); //to change visibility from visible to invisible
        eventanimation.setDuration(1000); //1 second duration for each animation cycle
        eventanimation.setInterpolator(new LinearInterpolator());
        eventanimation.setRepeatCount(Animation.INFINITE); //repeating indefinitely
        eventanimation.setRepeatMode(Animation.REVERSE); //animation will start from end point once ended.
        eventimage.startAnimation(eventanimation); //to start animation
        final Button BtnEvent = activity.findViewById(R.id.event);
        switch(eventtype){
            case 0: //egg hatching
                BtnEvent.setOnClickListener(v -> {
                    eventimage.setVisibility(View.INVISIBLE);
//                    currentmonster.hatch();
//                    currentmonster.evolve(getApplicationContext());
//                    int selectedmonster = currentmonster.getArrayid();
//                    selectedIcon(selectedmonster, activity);
                    AsyncTask.execute(() -> {
                        AppDatabase db = AppDatabase.buildDatabase(activity);
                        Monster tempmonster = db.journeyDao().getMonster().get(0);
                        tempmonster.hatch();
                        //no discount since we are just hatching
                        tempmonster.evolve(activity, 0);
                        //int selectedmonster = tempmonster.getArrayid();
                        //selectedIcon(selectedmonster, activity);
                        Journey temp = db.journeyDao().getJourney().get(0);
                        db.journeyDao().updateMonster(tempmonster);
                        currentmonster = tempmonster;
                        Random ran = new Random();
                        temp.setEventsteps(ran.nextInt(1500) + 500);
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
                        AppDatabase db = AppDatabase.buildDatabase(activity);
                        Journey temp = db.journeyDao().getJourney().get(0);
                        temp.setEventsteps(ran.nextInt(1500) + 500);
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
                    FrameLayout frmlayout = findViewById(R.id.placeholder);
                    LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                    assert aboutinflater != null;
                    final View food = aboutinflater.inflate(R.layout.feeding_screen, (ViewGroup)null);
                    food.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.basic_background));
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
                            @StyleableRes int index = 4;
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
                    eventimage.setVisibility(View.INVISIBLE);
                    eventanimation.cancel();
                    prepareBattle(true);
//                        AsyncTask.execute(new Runnable() {
//                                              @Override
//                                              public void run() {
//                                                  Journey temp = db.journeyDao().getJourney().get(0);
//                                                  Random ran = new Random();
//                                                  temp.setEventsteps(ran.nextInt(1500) + 500);
//                                                  if(ran.nextFloat() < 0.8){
//                                                      temp.setEventtype(2);
//                                                  }
//                                                  else{
//                                                      temp.setEventtype(1);
//                                                  }
//                                                  temp.setEventreached(false);
//                                                  db.journeyDao().update(temp);
//                                              }
//                                          });
                    BtnEvent.setVisibility(View.INVISIBLE);
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
                    //TODO randomly select 1 of 6 candidates of different monster types, play animation, and give a popup to decide if we will breed with them or not
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
                        AppDatabase db = AppDatabase.buildDatabase(this);
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
                    match.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.basic_background));
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
                    eventimage.setVisibility(View.INVISIBLE);
                    eventanimation.cancel();
                    //TODO animation for enemy before battle starts, then if boss defeated open up map screen and give player option to choose a new map
                    prepareBattle(true);

                    BtnEvent.setVisibility(View.INVISIBLE);
                });
                //TODO add logic for when boss is found, confirm
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
            home.getViewTreeObserver().addOnGlobalLayoutListener(() -> selectedIcon(currentmonster.getArrayid(), homeactivity));

            if(!accepted.get()){
                AsyncTask.execute(() -> {
                    AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
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
            //TODO call breed on current monster, then retire, set current monster to new egg, and refresh screen
            //TODO add finishing touches for match/breed confirm
            //TODO since eggs are for a certain type can make it so monsters save the type they are as an egg, then the egg can contain more type info if needed
            accepted.set(true);
            int selectedid = currentmonster.breed(matchedarray, getApplicationContext());
            AsyncTask.execute(()->{
                AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
                //update retirement info
                History temphistory = new History(currentmonster.getGeneration(), currentmonster.getArrayid(), currentmonster.getName());
                db.journeyDao().insertHistory(temphistory);

                // reset journey info
                Journey temp = db.journeyDao().getJourney().get(0);
                temp.setEventsteps(100);
                temp.setEventtype(0);
                temp.setFirsttime(false);
                temp.setEventreached(false);
                temp.setMatching(false);
                db.journeyDao().update(temp);

                //add egg to unlocked list
                List<UnlockedMonster> unlockedMonsters = db.journeyDao().getUnlockedMonster();
                for(UnlockedMonster unlockedMonster : unlockedMonsters){
                    if(unlockedMonster.getMonsterarrayid() == selectedid){
                        unlockedMonster.setDiscovered(true);
                        unlockedMonster.setUnlocked(true);
                    }
                }
                db.journeyDao().updateUnlockedMonster(unlockedMonsters);

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
    static private void monster_hatched(Activity activity){
        //TODO back button will close window, but if window is not focusable then edittext cannot be selected
        final LayoutInflater aboutinflater = (LayoutInflater)
                activity.getSystemService(LAYOUT_INFLATER_SERVICE);
        assert aboutinflater != null;
        View hatchedView = aboutinflater.inflate(R.layout.hatched_popup, activity.findViewById(R.id.parent), false);
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
                AppDatabase db = AppDatabase.buildDatabase(activity);
                Monster temp = db.journeyDao().getMonster().get(0);
                temp.setName(monstername);
                db.journeyDao().updateMonster(temp);
            });
            aboutWindow.dismiss();
        });
        hatchedView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //TODO test to confirm this works
                MainActivity.HatchedInfo runner = new MainActivity.HatchedInfo(activity, hatchedView.findViewById(R.id.monster_popup_icon));
                runner.execute();
                hatchedView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    /**
     * called when we have successfully defeated the boss,
     * resets steps to max, completes the chapter, and gives a reward based on if chapter has been completed or not
     */
    private void bossDefeated(){
        AsyncTask.execute(() -> {
            AppDatabase db = AppDatabase.buildDatabase(this);
            List<CompletedMaps> completedMapsList = db.journeyDao().getCompletedMaps();
            Journey temp = db.journeyDao().getJourney().get(0);
            //TODO add logic for unlocking new stuff, as well as congratulations popup that opens map
            for (CompletedMaps completedMaps : completedMapsList){
                if(completedMaps.getMaparray() == temp.getStorytype()){
                    if(completedMaps.isStorycompleted()){
                        //TODO give item for completing, maybe medicine used for evolution
                        db.journeyDao().insertItem(new Item(4));
                    }
                    else{
                        completedMaps.setStorycompleted(true);
                        //TODO unlock new egg/reduce steps needed to evolve a monster
                    }

                    temp.setStorysteps(completedMaps.getStorysteps());


                    break;
                }
            }
            db.journeyDao().update(temp);
            db.journeyDao().updateCompletedMaps(completedMapsList);
        });

        //open the map so player can choose a new spot to explore
        map();
    }

    /**
     * set the button info for which number picker scrolled to
     */
    private void selectedpick(){
        final int valuePicker = picker.getValue();
        String temp = picker.getDisplayedValues()[valuePicker];

        selectedpick.setText(temp);
        selectedpick.setOnClickListener(v -> {
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
                    minigame();
                    break;
                case 4:
                    connect();
                    break;
                case 5:
                    store();
                    break;
            }


        });
    }

    /**
     * training popup to gather energy which prepares for the battle
     */
    public void prepareBattle(final boolean first){
        if(trainView != null){
            return;
        }
        if(first){
            currentmonster.initializebattlestats(getApplicationContext());
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
        trainWindow.showAtLocation(findViewById(R.id.placeholder), Gravity.CENTER, 0, 0);
        final TextView trainingtitle = trainView.findViewById(R.id.training_title);
        final TextView trainingtime = trainView.findViewById(R.id.training_time);
        trainingtitle.setText(getText(R.string.TrainingReady));
        trainingtapcount = 0;
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
                        if(first){
                            //currentmonster.initializebattlestats(getApplicationContext());
                            TypedArray array = getResources().obtainTypedArray(currentstoryarray);
                            int[] monsterresources = getResources().getIntArray(currentmonster.getArrayid());
                            int stage = monsterresources[0];
                            Random ran = new Random();
                            TypedArray foundenemylist = getResources().obtainTypedArray(array.getResourceId(stage-1, R.array.basic_dino_enemies));
                            int[] possibleenemies = getResources().getIntArray(array.getResourceId(stage-1, R.array.child_dino_enemies));
                            enemyarrayid = foundenemylist.getResourceId(ran.nextInt(possibleenemies[0]+1), R.array.dino_baby1);
                            array.recycle();
                            foundenemylist.recycle();
                            int[] enemyfound = getResources().getIntArray(enemyarrayid);
                            enemyhealth = enemyfound[5+enemyfound[1]] - 1;
                            enemymaxhealth = enemyfound[5+enemyfound[1]] - 1;
                        }
                        int[] enemyfound = getResources().getIntArray(enemyarrayid);
                        int enemyattack = enemyfound[6+enemyfound[1]];
                        int enemychance = enemyfound[7+enemyfound[1]];
                        performbattle(currentmonster.battle(enemyattack,enemyhealth,enemychance, trainingtapcount, false),
                                enemyattack);
                        AsyncTask.execute(() -> {
                            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
                            Journey temp = db.journeyDao().getJourney().get(0);
                            temp.setEnemyarrayid(enemyarrayid);
                            temp.setIsbattling(true);
                            db.journeyDao().update(temp);
                            db.journeyDao().updateMonster(currentmonster);
                        });
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
    public void performbattle(final ArrayList<Integer> rounds, final int enemyattackvalue){

        //create popup display for monster stats durring battle
        LayoutInflater battleinflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert battleinflater != null;
        battleView = battleinflater.inflate(R.layout.battle_info, findViewById(R.id.parent),false);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        //boolean focusable2 = true; // lets taps outside the popup also dismiss it
        final PopupWindow battleWindow = new PopupWindow(battleView, width2, height2, false);
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
        playerhealthfill.setLevel((int)(10000*(float)(currentmonster.getCurrenthealth()/currentmonster.getMaxhealth())));

        //show our animation for each attack
        final FrameLayout frmlayout = findViewById(R.id.placeholder);
        LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        assert aboutinflater != null;
        final View battle = aboutinflater.inflate(R.layout.battlescreen, null);
        Fade mFade = new Fade(Fade.IN);
        TransitionManager.beginDelayedTransition(frmlayout, mFade);

        frmlayout.removeAllViews();
        frmlayout.addView(battle,0);

        final ImageView monster = battle.findViewById(R.id.monster_icon);
        final ImageView attack1View = battle.findViewById(R.id.myattack);
        final ImageView attack2View = battle.findViewById(R.id.theirattack);
                //add our different animations together to play
        AnimatorSet battleAnimation = new AnimatorSet();
//                ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(
//                        imageView, "translationX", 1.05f);
//                scaleUpX.setDuration(1000L);


                ArrayList<Animator> attackanimations = new ArrayList<>();
                @StyleableRes int index = 4;
                //use our r.array id to find array for current monster
                TypedArray array1 = getApplicationContext().getResources().obtainTypedArray(currentmonster.getArrayid());
                TypedArray array2 = getApplicationContext().getResources().obtainTypedArray(enemyarrayid);
                final int player1 = array1.getResourceId(index,R.drawable.egg_idle);
                final int player2 = array2.getResourceId(index,R.drawable.egg_idle);
                array1.recycle();
                array2.recycle();

                for(Integer round : rounds){
                    boolean damagedanimation = false;
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
                            playerattack.addUpdateListener(animation -> {
                                monster.setVisibility(View.VISIBLE);
                                monster.setScaleX(1);
                                final float progress = (float) animation.getAnimatedValue();
                                monster.setBackgroundResource(player1);
                                if(progress > 0.7f){
                                    attack1View.setVisibility(View.VISIBLE);
                                    float width =-(progress)* battle.getWidth();
                                    attack1View.setTranslationX(width);
                                }
                                else{
                                    attack1View.setVisibility(View.INVISIBLE);
                                    attack2View.setVisibility(View.INVISIBLE);
                                }
                            });
                            player2attack.addUpdateListener(animation -> {
                                monster.setVisibility(View.VISIBLE);
                                monster.setScaleX(-1);
                                final float progress = (float) animation.getAnimatedValue();
                                monster.setBackgroundResource(player2);
                                if(progress > 0.7f){
                                    attack2View.setVisibility(View.VISIBLE);
                                    float width =(progress)* battle.getWidth();
                                    attack2View.setTranslationX(width);
                                }
                                else{
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
                                attack1View.setTranslationX(-width);
                                attack2View.setTranslationX(width);
                            });


                            break;
                        case 1: //p1 win
                            playerattack.addUpdateListener(animation -> {
                                monster.setVisibility(View.VISIBLE);
                                monster.setScaleX(1);
                                final float progress = (float) animation.getAnimatedValue();
                                monster.setBackgroundResource(player1);
                                if(progress > 0.7f){
                                    attack1View.setVisibility(View.VISIBLE);
                                    float width =-(progress)* battle.getWidth();
                                    attack1View.setTranslationX(width);
                                }
                                else{
                                    attack1View.setVisibility(View.INVISIBLE);
                                    attack2View.setVisibility(View.INVISIBLE);
                                }
                            });
                            player2attack.addUpdateListener(animation -> {
                                monster.setVisibility(View.VISIBLE);
                                monster.setScaleX(-1);
                                monster.setBackgroundResource(player2);
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
                                    monster.setBackgroundResource(player2);
                                    attack1View.setVisibility(View.VISIBLE);
                                    float width =-progress* battle.getWidth();
                                    attack1View.setTranslationX(width);
                                }
                                else{
                                    monster.setBackgroundResource(R.drawable.damaged);
                                    AnimationDrawable tempanimator = (AnimationDrawable) monster.getBackground();
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
                                monster.setVisibility(View.VISIBLE);
                                monster.setScaleX(1);
                                monster.setBackgroundResource(player1);
                                attack1View.setVisibility(View.INVISIBLE);
                                attack2View.setVisibility(View.INVISIBLE);
                            });
                            player2attack.addUpdateListener(animation -> {
                                monster.setVisibility(View.VISIBLE);
                                monster.setScaleX(-1);
                                final float progress = (float) animation.getAnimatedValue();
                                monster.setBackgroundResource(player2);
                                if(progress > 0.7f){
                                    attack2View.setVisibility(View.VISIBLE);
                                    float width =(progress)* battle.getWidth();
                                    attack2View.setTranslationX(width);
                                }
                                else{
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
                                    monster.setBackgroundResource(player1);
                                    attack2View.setVisibility(View.VISIBLE);
                                    float width =progress* battle.getWidth();
                                    attack2View.setTranslationX(width);
                                }
                                else{
                                    monster.setBackgroundResource(R.drawable.damaged);
                                    AnimationDrawable tempanimator = (AnimationDrawable) monster.getBackground();
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

                    //battleAnimation.play(attackanimator);

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
                        battleWindow.dismiss();
                        if(enemyhealth > 0 && currentmonster.getCurrenthealth() > 0){
                            prepareBattle(false);
                        }else{
                            //TODO Add win/loss animation or continue animation
                            frmlayout.removeAllViews();
                            LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                            assert aboutinflater != null;
                            final View home = aboutinflater.inflate(R.layout.home_screen, (ViewGroup)null);
                            Fade mFade = new Fade(Fade.IN);
                            TransitionManager.beginDelayedTransition(frmlayout, mFade);

                            frmlayout.addView(home);
                            home.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {

                                    selectedIcon(currentarrayid, mainActivity);

                                    home.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }
                            });

                            AsyncTask.execute(() -> {
                                AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
                                Journey temp = db.journeyDao().getJourney().get(0);
                                temp.setIsbattling(false);
                                Random ran = new Random();
                                temp.setEventsteps(ran.nextInt(1500) + 500);
                                if(ran.nextFloat() < 0.8){
                                    temp.setEventtype(2);
                                }
                                else{
                                    temp.setEventtype(1);
                                }
                                temp.setEventreached(false);
                                db.journeyDao().update(temp);
                            });
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

    //resume our activity
    static private class AsyncResume extends AsyncTask<String,TextView,String>{
        // Weak references will still allow the Activity to be garbage-collected
        private final WeakReference<Activity> weakActivity;

        public AsyncResume(Activity myActivity){
            this.weakActivity = new WeakReference<>(myActivity);
        }
        private int eventtype;
        private long stepsneeded, storysteps;
        private boolean isEventreached = false;
        private long matchsteps = 1000;
        private String message;
        private String monstername;
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(weakActivity.get());
            String event = (String)weakActivity.get().getText(R.string.stepsneeded);
            Journey temp = db.journeyDao().getJourney().get(0);
            monstername = db.journeyDao().getMonster().get(0).getName();
            if(db.journeyDao().getMonster().get(0).getHatched()){
                stepsneeded = temp.getStorysteps();
            }else{
                stepsneeded = temp.getEventsteps();
            }
            isEventreached = temp.isEventreached();
            message = event + stepsneeded;
            eventtype = temp.getEventtype();
            storysteps = temp.getStorysteps();
            MainActivity mainActivity = (MainActivity) weakActivity.get();
            mainActivity.currentarrayid = db.journeyDao().getMonster().get(0).getArrayid();
            if(temp.isMatching()){
                matchsteps = temp.getMatchmakersteps();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            //TODO add if check for when boss encountered
            TextView TvSteps = weakActivity.get().findViewById(R.id.tv_steps);
            TextView MonsterName = weakActivity.get().findViewById(R.id.monster_name);
            TvSteps.setText(message);
            MonsterName.setText(monstername);
            MainActivity mainActivity = (MainActivity) weakActivity.get();
            if(storysteps <= 0){
                //event 4 is for when boss is found
                mainActivity.startEvent(4,weakActivity.get());
            }
            if(stepsneeded <= 0 && isEventreached) {
                mainActivity.startEvent(eventtype,weakActivity.get());
            }
            else if(matchsteps <= 0){
                mainActivity.startEvent(3,weakActivity.get());
            }
        }
    }

    //fetch and display the monster info for current status
    private class MonsterInfo extends AsyncTask<String,TextView,String>{

        private int hunger;
        private int training;
        private String name;
        private int arrayid;
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            Monster temp = db.journeyDao().getMonster().get(0);
            hunger = temp.getHunger();
            training = temp.getDiligence();
            name = temp.getName();
            arrayid = temp.getArrayid();
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

            TextView nameView = aboutView.findViewById(R.id.monster_popup_name);
            nameView.setText(name);

            @StyleableRes int index = 4;
            TypedArray array = getApplicationContext().getResources().obtainTypedArray(arrayid);
            int resource = array.getResourceId(index,R.drawable.egg_idle);
            array.recycle();
            ImageView infoView = aboutView.findViewById(R.id.monster_popup_icon);
            infoView.setBackgroundResource(resource);

            AnimationDrawable infoanimator = (AnimationDrawable) infoView.getBackground();
            infoanimator.start();
        }
    }

    static private class HatchedInfo extends AsyncTask<String,TextView,String> {
        private int arrayid;
        private View viewid;
        // Weak references will still allow the Activity to be garbage-collected
        private final WeakReference<Activity> weakActivity;

        public HatchedInfo(Activity myActivity, View viewid){
            this.weakActivity = new WeakReference<>(myActivity);
            this.viewid = viewid;
        }

        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(weakActivity.get());
            Monster temp = db.journeyDao().getMonster().get(0);
            arrayid = temp.getArrayid();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            @StyleableRes int index = 4;
            TypedArray array = weakActivity.get().getApplicationContext().getResources().obtainTypedArray(arrayid);
            int resource = array.getResourceId(index,R.drawable.egg_idle);
            array.recycle();
            //TODO test to confirm this works

            ImageView infoView = viewid.findViewById(R.id.monster_popup_icon);
            infoView.setBackgroundResource(resource);
            AnimationDrawable infoanimator = (AnimationDrawable) infoView.getBackground();
            infoanimator.start();
        }
    }

    /**
     * AsyncTask to start the care
     */
    private class StartTraining extends AsyncTask<String,TextView,String> {
        private boolean hatched;
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            Monster temp = db.journeyDao().getMonster().get(0);
            hatched = temp.getHatched();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if(hatched){
                //if(!isplaying)music.pause();
                Intent intent = new Intent(getApplicationContext(), Training.class);
                startActivity(intent);
                //where right side is current view
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
            else {
                Toast.makeText(getApplicationContext(), "Hatch your monster first.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * AsyncTask to start the care
     */
    static private class DisplayMonster extends AsyncTask<String,TextView,String> {
        private int arrayid;
        // Weak references will still allow the Activity to be garbage-collected
        private final WeakReference<Activity> weakActivity;

        public DisplayMonster(Activity myActivity){
            this.weakActivity = new WeakReference<>(myActivity);
        }
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(weakActivity.get());
            arrayid = db.journeyDao().getMonster().get(0).getArrayid();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            MainActivity activity = (MainActivity) weakActivity.get();
            activity.selectedIcon(arrayid,weakActivity.get());
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
        View confirmView = confirminflater.inflate(R.layout.confirm_popup, null);
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
}
