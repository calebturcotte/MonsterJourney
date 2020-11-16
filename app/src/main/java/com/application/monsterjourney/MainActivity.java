package com.application.monsterjourney;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ClipDrawable;
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "MyJourneyFile";
    SharedPreferences settings;
    public long stepscounted;
    private TextView totaltime;
    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    private TextView TvSteps, MonsterName;
    private Button BtnStart, BtnStop, BtnEvent;
    private boolean runbackground;

    private AnimationDrawable monsteranimator;
    private ImageView imageView;
    private int currentgeneration;
    private Monster currentmonster;
    private boolean hatched;

    private int currentarrayid;
    private int currentstoryarray;
    private int currentevent;
    private boolean eventreached;

    private NumberPicker picker;
    private Button selectedpick;
    private AppDatabase db;

    public ImageView eventimage;

    public View aboutView, trainView, battleView, optionsView, storeView;
    public int trainingtapcount;

    private int enemyarrayid, enemyhealth, enemymaxhealth;

    private AdView mAdView;
    private BillingClient billingClient;
    //private String[] skuList = {"remove_advertisements", "purchase_light_egg", "purchase_dark_egg", "purchase_cosmic_egg", "purchase_item_bundle"};
    private List<SkuDetails> skuList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = getSharedPreferences(PREFS_NAME, 0);
        TvSteps = (TextView) findViewById(R.id.tv_steps);
        MonsterName = (TextView) findViewById(R.id.monster_name);
        BtnStart = (Button) findViewById(R.id.btn_start);
        BtnStop = (Button) findViewById(R.id.btn_stop);
        BtnEvent = (Button) findViewById(R.id.event);
        totaltime = findViewById(R.id.total_time);
        picker = (NumberPicker) findViewById(R.id.picker);
        selectedpick = (Button) findViewById(R.id.selection);

        boolean isbought = settings.getBoolean("isbought", false);

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
                    mAdView.setVisibility(View.GONE);
                }

                String[] pickervals = new String[]{"Library", "Map", "Care", "Minigame","Connect", "Shop"};

                picker.setMaxValue(5);
                picker.setMinValue(0);

                picker.setDisplayedValues(pickervals);

                picker.setOnValueChangedListener((numberPicker, i, i1) -> selectedpick());

                selectedpick();


                mAdView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        runbackground = settings.getBoolean("runbackground", true);



        //TODO remove/replace ui for something else since background process is toggled in options now
//        BtnStart.setOnClickListener(arg0 -> {
//            runbackground = true;
//            SharedPreferences.Editor editor = settings.edit();
//            editor.putBoolean("runbackground", runbackground);
//            editor.apply();
//
//        });
//
//        BtnStop.setOnClickListener(arg0 -> {
//            runbackground = false;
//            SharedPreferences.Editor editor = settings.edit();
//            editor.putBoolean("runbackground", runbackground);
//            editor.apply();
//
//        });

        //findViewById(R.id.test).setOnClickListener(v -> prepareBattle(true));

        //findViewById(R.id.game_options).setOnClickListener(v -> options());

        ForeGroundService mForeGroundService = new ForeGroundService();
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

                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                db = AppDatabase.buildDatabase(getApplicationContext());
                                //handleEvent();
                            }
                        });
                        handleEvent();
                    }
                }, new IntentFilter(ForeGroundService.ACTION_BROADCAST)
        );

        //add our home screen with the current monster
        final FrameLayout frmlayout = (FrameLayout) findViewById(R.id.placeholder);
        LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final View home = aboutinflater.inflate(R.layout.home_screen, (ViewGroup)null);
        Fade mFade = new Fade(Fade.IN);
        TransitionManager.beginDelayedTransition(frmlayout, mFade);

        frmlayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                frmlayout.addView(home,0);

                imageView = home.findViewById(R.id.monster_icon);
                imageView.setBackgroundResource(R.drawable.egg_idle);
                monsteranimator = (AnimationDrawable) imageView.getBackground();

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
            db = AppDatabase.buildDatabase(getApplicationContext());
            Journey tempjourney = db.journeyDao().getJourney().get(0);
            totaltime.setText(String.valueOf(tempjourney.getTotalsteps()));
            currentmonster = db.journeyDao().getMonster().get(0);
            currentarrayid = currentmonster.getArrayid();

            boolean firsttime = tempjourney.isFirsttime();
            if (firsttime) {
                eggSelect();
            }
        });

        setupBillingClient();

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
        optionsView.findViewById(R.id.close).setOnClickListener(v -> {
            optionsWindow.dismiss();
            optionsView = null;
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


    }

    @Override
    protected void onResume() {
        findViewById(R.id.placeholder).getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                AsyncTask.execute(new Runnable() {
                                      @Override
                                      public void run() {
                                          db = AppDatabase.buildDatabase(getApplicationContext());
                                          currentarrayid = db.journeyDao().getMonster().get(0).getArrayid();
                                          currentstoryarray = db.journeyDao().getJourney().get(0).getStorytype();
                                          selectedIcon(currentarrayid);
                                          //handleEvent(settings.getInt("eventtype",0));
                                          //handleEvent();
                                      }
                                  });
                handleEvent();
                findViewById(R.id.placeholder).getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        super.onResume();
    }

    /**
     * start our foreground step tracking service
     */
    public void startService() {
        Intent serviceIntent = new Intent(this, ForeGroundService.class);
        serviceIntent.putExtra("inputExtra", "Foreground Service Example in Android");

        ContextCompat.startForegroundService(this, serviceIntent);
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
        monsteranimator.start();
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
        //if(!isplaying)music.pause();
        Intent intent = new Intent(MainActivity.this, Communication.class);
        startActivity(intent);
        //where right side is current view
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
            // To be implemented in a later section.
        }
    };

    public void startConnection(){
//        if(billingClient == null){
//            return;
//        }
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                //startConnection();
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


//        BillingClient billingClient = BillingClient.newBuilder(this)
//                .setListener(purchasesUpdatedListener)
//                .enablePendingPurchases()
//                .build();

        startConnection();

        List<String> skuList = new ArrayList<> ();
        skuList.add("remove_advertisements");
        skuList.add("purchase_light_egg");
        skuList.add("purchase_dark_egg");
        skuList.add("purchase_cosmic_egg");
        skuList.add("purchase_item_bundle");

        ArrayList<SkuDetails> details = new ArrayList<>();
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
        billingClient.querySkuDetailsAsync(params.build(),
                (billingResult, skuDetailsList) -> {
                    // Process the result.
                    //assert skuDetailsList != null;
                    //details.addAll(skuDetailsList);
                });


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
        storeView.findViewById(R.id.close).setOnClickListener(v -> {
            storeWindow.dismiss();
            storeView = null;
        });

        storeView.findViewById(R.id.buyitempack).setOnClickListener(v -> {
//            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
//                    .setSkuDetails("buy_item_pack")
//                    .setType(BillingClient.SkuType.INAPP)
//                    .build();
            //int responseCode = billingClient.launchBillingFlow(this, flowParams);
//            storeWindow.dismiss();
//            storeView = null;

            // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
            //SkuDetails skuDetails;
            List<String> skuList2 = new ArrayList<> ();
            skuList2.add("purchase_item_bundle");

            SkuDetailsParams.Builder params2 = SkuDetailsParams.newBuilder();
            params2.setSkusList(skuList2).setType(BillingClient.SkuType.INAPP);
            billingClient.querySkuDetailsAsync(params2.build(),
                    (billingResult, skuDetailsList) -> {
                        // Process the result.
                        //can't test purchases on emulator, must be part of an alpha test track
                        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                .setSkuDetails(skuDetailsList.get(0))
                                .build();
                        int responseCode = billingClient.launchBillingFlow(this, billingFlowParams).getResponseCode();
                    });
//            for(SkuDetails skuDetail : details){
//                if(skuDetail.getSku().equals("buy_item_pack")){
//                    BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
//                            .setSkuDetails(skuDetail)
//                            .build();
//                    int responseCode = billingClient.launchBillingFlow(this, billingFlowParams).getResponseCode();
//                    break;
//                }
//            }

        });
    }

    /**
     * set up the billing client used for purchases
     */
    private void setupBillingClient(){
        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases()
                .setListener(purchasesUpdatedListener)
                .build();

//        billingClient.startConnection(new BillingClientStateListener() {
//            @Override
//            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
//                // The BillingClient is setup successfully
//            }
//
//            @Override
//            public void onBillingServiceDisconnected() {
//                // Try to restart the connection on the next request to
//                // Google Play by calling the startConnection() method.
//            }
//        });

        //        BillingClient billingClient = BillingClient.newBuilder(this)
//                .setListener(purchasesUpdatedListener)
//                .enablePendingPurchases()
//                .build();
    }

//    /**
//     * Our listener for when a purchase is updated
//     * @param billingResult result of the purchase
//     * @param list list of purchases
//     */
//    @Override
//    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
//        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
//                && list != null) {
//            for (Purchase purchase : list) {
//                switch(purchase.getSku()){
//                    case "remove_advertisements":
//                        SharedPreferences.Editor editor = settings.edit();
//                        editor.putBoolean("isbought", true);
//                        editor.apply();
//                        mAdView.setVisibility(View.GONE);
//                        break;
//                    case "purchase_dark_egg":
//                        break;
//                    case "purchase_light_egg":
//                        break;
//                    case "purchase_cosmic_egg":
//                        break;
//                    case "purchase_item_bundle":
//                        AsyncTask.execute(() -> {
//                            AppDatabase db = AppDatabase.buildDatabase(this);
//                            Item[] itemlist = {
//                                    new Item(2),
//                                    new Item(2),
//                                    new Item(2),
//                                    new Item(2),
//                                    new Item(2),
//                                    new Item(3),
//                                    new Item(3),
//                                    new Item(3),
//                                    new Item(3),
//                                    new Item(3),
//                                    new Item(4),
//                                    new Item(4),
//                                    new Item(4),
//                                    new Item(4),
//                                    new Item(4)
//                            };
//
//                            db.journeyDao().insertAllItems(itemlist);
//                        });
//                        break;
//                }
//                if (purchase.getSku().equals("remove_all_advertisements")) {
//
//                    // Unlock the the premium app features and hide the buyBtn
//                }
//            }
//        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
//            // Handle an error caused by a user cancelling the purchase flow.
//        } else {
//            // Handle any other error codes.
//        }
//    }


    /**
     * shows the icon for our current monster
     * @param selected the selected icon for our monster
     */
    private void selectedIcon(int selected){
        @StyleableRes int index = 4;
        //use our r.array id to find array for current monster
        TypedArray array = getApplicationContext().getResources().obtainTypedArray(selected);
        int resource = array.getResourceId(index,R.drawable.egg_idle);
        array.recycle();
        imageView = findViewById(R.id.monster_icon);
        imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), resource));
        imageView.setBackgroundResource(R.drawable.basic_background);

        monsteranimator = (AnimationDrawable) imageView.getDrawable();
        monsteranimator.start();
    }

    /**
     * handles steps taken for our event
     */
    private void handleEvent(){
        AsyncResume runner = new AsyncResume();
        runner.execute();
    }

    /**
     * code for when our monster has reached an event of some sort
     * @param eventtype the type of event that happened, egg hatching, battle/item found etc.
     */
    public void startEvent(int eventtype){
        eventimage = findViewById(R.id.monster_event);
        eventimage.setVisibility(View.VISIBLE);
        final Animation eventanimation = new AlphaAnimation(1, 0); //to change visibility from visible to invisible
        eventanimation.setDuration(1000); //1 second duration for each animation cycle
        eventanimation.setInterpolator(new LinearInterpolator());
        eventanimation.setRepeatCount(Animation.INFINITE); //repeating indefinitely
        eventanimation.setRepeatMode(Animation.REVERSE); //animation will start from end point once ended.
        eventimage.startAnimation(eventanimation); //to start animation
        final Button BtnEvent = findViewById(R.id.event);
        switch(eventtype){
            case 0: //egg hatching
                BtnEvent.setOnClickListener(v -> {
                    eventimage.setVisibility(View.INVISIBLE);
                    currentmonster.hatch();
                    currentmonster.evolve(getApplicationContext());
                    int selectedmonster = currentmonster.getArrayid();
                    selectedIcon(selectedmonster);
                    AsyncTask.execute(() -> {
                        db = AppDatabase.buildDatabase(getApplicationContext());
//                                Monster tempmonster = db.journeyDao().getMonster().get(0);
//                                tempmonster.hatch();
//                                tempmonster.evolve(getApplicationContext());
//                                int selectedmonster = tempmonster.getArrayid();
//                                selectedIcon(selectedmonster);
                        Journey temp = db.journeyDao().getJourney().get(0);
                        db.journeyDao().updateMonster(currentmonster);
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
                            if(unlockedMonster.getMonsterarrayid() == currentmonster.getArrayid()){
                                unlockedMonster.setUnlocked(true);
                                unlockedMonster.setDiscovered(true);
                            }
                        }
                        db.journeyDao().updateUnlockedMonster(unlockedMonsters);
//                                db.journeyDao().updateMonster(tempmonster);
                    });
                    eventanimation.cancel();
                    monster_hatched();
                    BtnEvent.setVisibility(View.INVISIBLE);
                });
                String evolve = "EVOLVE";
                BtnEvent.setText(evolve);
                BtnEvent.setVisibility(View.VISIBLE);
                break;
            case 1: //item found
                BtnEvent.setOnClickListener(v -> {
                    eventimage.setVisibility(View.INVISIBLE);
                    eventanimation.cancel();
                    AsyncTask.execute(() -> {
                        Journey temp = db.journeyDao().getJourney().get(0);
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
                        Item tempitem= new Item(1);
                        db.journeyDao().insertItem(tempitem);
                    });
                    BtnEvent.setVisibility(View.INVISIBLE);
                });
                String itemevent = "ITEM FOUND";
                BtnEvent.setText(itemevent);
                BtnEvent.setVisibility(View.VISIBLE);
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
                String event = "ENEMY FOUND";
                BtnEvent.setText(event);
                BtnEvent.setVisibility(View.VISIBLE);
                break;
            case 3://match found
                String match = "MATCH FOUND";
                break;
            case 4: //boss found
                String boss = "BOSS FOUND";
                break;
        }

    }

    /**
     * code for entering name data when monster was hatched
     */
    private void monster_hatched(){
        final LayoutInflater aboutinflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert aboutinflater != null;
        aboutView = aboutinflater.inflate(R.layout.hatched_popup, null);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        boolean focusable2 = true; // lets taps outside the popup also dismiss it
        final PopupWindow aboutWindow = new PopupWindow(aboutView, width2, height2, focusable2);


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            aboutWindow.setElevation(20);
        }
        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        aboutWindow.setAnimationStyle(R.style.PopupAnimation);
        aboutWindow.showAtLocation(findViewById(R.id.monster_info_popup), Gravity.CENTER, 0, 0);
        aboutView.findViewById(R.id.close).setOnClickListener(v -> aboutWindow.dismiss());
        aboutView.findViewById(R.id.name_button).setOnClickListener(v -> {
            EditText textbox = aboutView.findViewById(R.id.name_enter);
            final String monstername = textbox.getText().toString();
            AsyncTask.execute(() -> {
                db = AppDatabase.buildDatabase(getApplicationContext());
                Monster temp = db.journeyDao().getMonster().get(0);
                temp.setName(monstername);
                db.journeyDao().updateMonster(temp);
            });
            aboutWindow.dismiss();
        });
        aboutView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                MainActivity.HatchedInfo runner = new MainActivity.HatchedInfo();
                runner.execute();
                aboutView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });


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
        final PopupWindow trainWindow = new PopupWindow(trainView, width2, height2, true);
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
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
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

                        Handler h = new Handler();
                        //Run a runnable after 100ms (after that time it is safe to remove the view)
                        h.postDelayed(() -> {
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
            }
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
        battleView = battleinflater.inflate(R.layout.battle_info, null);
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
        final FrameLayout frmlayout = (FrameLayout) findViewById(R.id.placeholder);
        LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final View battle = aboutinflater.inflate(R.layout.battlescreen, (ViewGroup)null);
        Fade mFade = new Fade(Fade.IN);
        TransitionManager.beginDelayedTransition(frmlayout, mFade);

                frmlayout.removeAllViews();
                frmlayout.addView(battle,0);

                final ImageView monster = battle.findViewById(R.id.monster_icon);
                final ImageView attack1View = battle.findViewById(R.id.myattack);
                final ImageView attack2View = battle.findViewById(R.id.theirattack);
                //add our different animations together to play
                AnimatorSet battleAnimation = new AnimatorSet();
                ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(
                        imageView, "translationX", 1.05f);
                scaleUpX.setDuration(1000L);


                ArrayList<Animator> attackanimations = new ArrayList<Animator>();
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
                                final float progress = (float) animation.getAnimatedValue();
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
                            attackanimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    monster.setVisibility(View.INVISIBLE);
                                    attack1View.setVisibility(View.INVISIBLE);
                                    attack2View.setVisibility(View.VISIBLE);
                                    final float progress = (float) animation.getAnimatedValue();
                                    float width = battle.getWidth()*progress;
                                    attack1View.setTranslationX(-width);
                                    attack2View.setTranslationX(width);
                                }
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
                imageView.setBackgroundResource(R.drawable.egg_idle);
                monsteranimator = (AnimationDrawable) imageView.getBackground();
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
                            final View home = aboutinflater.inflate(R.layout.home_screen, (ViewGroup)null);
                            Fade mFade = new Fade(Fade.IN);
                            TransitionManager.beginDelayedTransition(frmlayout, mFade);

                            frmlayout.addView(home);
                            home.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {

                                    selectedIcon(currentarrayid);

                                    home.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }
                            });

                            AsyncTask.execute(new Runnable() {
                                @Override
                                public void run() {
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
                                }
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
    private class AsyncResume extends AsyncTask<String,TextView,String>{

        private int eventtype;
        private long stepsneeded;
        private boolean isEventreached = false;
        private long matchsteps = 1000;
        private String message;
        private String monstername;
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            String event = "Steps needed: ";
            Journey temp = db.journeyDao().getJourney().get(0);
            // currentevent = temp.getEventtype();
            monstername = db.journeyDao().getMonster().get(0).getName();
            stepsneeded = temp.getEventsteps();
            isEventreached = temp.isEventreached();
            message = event + stepsneeded;
            eventtype = temp.getEventtype();
            if(temp.isMatching()){
                matchsteps = temp.getMatchmakersteps();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            TvSteps.setText(message);
            MonsterName.setText(monstername);
            // execution of result of Long time consuming operation
            if(stepsneeded <= 0 && isEventreached) {
                startEvent(eventtype);
            }
            else if(matchsteps <= 0){
                startEvent(3);
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

    private class HatchedInfo extends AsyncTask<String,TextView,String> {
        private int arrayid;
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            Monster temp = db.journeyDao().getMonster().get(0);
            arrayid = temp.getArrayid();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
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


}
