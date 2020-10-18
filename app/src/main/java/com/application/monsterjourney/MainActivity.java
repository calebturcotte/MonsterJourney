package com.application.monsterjourney;

import androidx.annotation.StyleableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity{
    public static final String PREFS_NAME = "MyJourneyFile";
    SharedPreferences settings;
    public long stepscounted;
    private TextView totaltime;
    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    private TextView TvSteps;
    private Button BtnStart, BtnStop, BtnEvent;
    private boolean runbackground;

    private AnimationDrawable monsteranimator;
    private ImageView imageView;
    private int currentgeneration;
    private Monster currentmonster;
    private boolean hatched;

    private int currentarrayid;
    private int currentevent;
    private boolean eventreached;

    private NumberPicker picker;
    private Button selectedpick;
    private AppDatabase db;

    public ImageView eventimage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TvSteps = (TextView) findViewById(R.id.tv_steps);
        BtnStart = (Button) findViewById(R.id.btn_start);
        BtnStop = (Button) findViewById(R.id.btn_stop);
        BtnEvent = (Button) findViewById(R.id.event);
        totaltime = findViewById(R.id.total_time);
        picker = (NumberPicker) findViewById(R.id.picker);
        selectedpick = (Button) findViewById(R.id.selection);

        String[] pickervals = new String[]{"Library", "Map", "Train", "Minigame","Connect"};

        picker.setMaxValue(4);
        picker.setMinValue(0);

        picker.setDisplayedValues(pickervals);

        picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                selectedpick();
            }
        });

        settings = getSharedPreferences(PREFS_NAME, 0);
        runbackground = settings.getBoolean("runbackground", true);




        BtnStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                runbackground = true;
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("runbackground", runbackground);
                editor.apply();

            }
        });

        BtnStop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                runbackground = false;
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("runbackground", runbackground);
                editor.apply();

            }
        });

        findViewById(R.id.test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<Integer> temp = new ArrayList<Integer>();
                temp.add(1);
                temp.add(2);
                temp.add(0);
                performbattle(currentmonster.battle(1,5,40));
            }
        });

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
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH){
            Fade mFade = new Fade(Fade.IN);
            TransitionManager.beginDelayedTransition(frmlayout, mFade);
        }
        frmlayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                frmlayout.addView(home,0);

                imageView = home.findViewById(R.id.monster_icon);
                imageView.setBackgroundResource(R.drawable.egg_idle);
                monsteranimator = (AnimationDrawable) imageView.getBackground();


                selectedpick();
                frmlayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                db = AppDatabase.buildDatabase(getApplicationContext());
                Journey tempjourney = db.journeyDao().getJourney().get(0);
                totaltime.setText(String.valueOf(tempjourney.getTotalsteps()));
                currentmonster = db.journeyDao().getMonster().get(0);
                currentarrayid = currentmonster.getArrayid();

                boolean firsttime = tempjourney.isFirsttime();
                if (firsttime) {
                    eggSelect();
                }
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
        //if(!isplaying)music.pause();
        Intent intent = new Intent(this, Training.class);
        startActivity(intent);
        //where right side is current view
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
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
        imageView.setBackgroundResource(resource);

        monsteranimator = (AnimationDrawable) imageView.getBackground();
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
                BtnEvent.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        eventimage.setVisibility(View.INVISIBLE);
                        currentmonster.hatch();
                        currentmonster.evolve(getApplicationContext());
                        int selectedmonster = currentmonster.getArrayid();
                        selectedIcon(selectedmonster);
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                db = AppDatabase.buildDatabase(getApplicationContext());
                                Monster tempmonster = db.journeyDao().getMonster().get(0);
                                tempmonster.hatch();
                                tempmonster.evolve(getApplicationContext());
                                int selectedmonster = tempmonster.getArrayid();
                                selectedIcon(selectedmonster);
                                Journey temp = db.journeyDao().getJourney().get(0);
                                db.journeyDao().updateMonster(currentmonster);
                                temp.setEventsteps(500);
                                temp.setEventtype(2);
                                temp.setEventreached(false);
                                db.journeyDao().update(temp);
                                db.journeyDao().updateMonster(tempmonster);
                            }
                        });
                        eventanimation.cancel();
                        BtnEvent.setVisibility(View.INVISIBLE);
                    }
                });
                String evolve = "EVOLVE";
                BtnEvent.setText(evolve);
                BtnEvent.setVisibility(View.VISIBLE);
                break;
            case 1: //item found
                break;

            case 2: //battle found
                BtnEvent.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        eventimage.setVisibility(View.INVISIBLE);
                        eventanimation.cancel();
                        int selectedmonster = currentmonster.getArrayid();
                        selectedIcon(selectedmonster);
                        AsyncTask.execute(new Runnable() {
                                              @Override
                                              public void run() {
                                                  Journey temp = db.journeyDao().getJourney().get(0);
                                                  temp.setEventsteps(500);
                                                  temp.setEventtype(2);
                                                  temp.setEventreached(false);
                                                  db.journeyDao().update(temp);
                                              }
                                          });
                        BtnEvent.setVisibility(View.INVISIBLE);
                    }
                });
                String event = "ENEMY FOUND";
                BtnEvent.setText(event);
                BtnEvent.setVisibility(View.VISIBLE);
                break;
        }

    }

    /**
     * set the button info for which number picker scrolled to
     */
    private void selectedpick(){
        final int valuePicker = picker.getValue();
        String temp = "";
        switch(valuePicker){
            case 0:
                temp = "Library";
                break;

            case 1:
                temp = "Map";
                break;
            case 2:
                temp = "Training";
                break;
            case 3:
                temp = "Minigame";
                break;
        }
        selectedpick.setText(temp);
        selectedpick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                }


            }
        });
    }

    /**
     * our animation for the battle performed
     */
    public void performbattle(final ArrayList<Integer> rounds){
        //show our animation for each attack
        final FrameLayout frmlayout = (FrameLayout) findViewById(R.id.placeholder);
        LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final View battle = aboutinflater.inflate(R.layout.battlescreen, (ViewGroup)null);
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH){
            Fade mFade = new Fade(Fade.IN);
            TransitionManager.beginDelayedTransition(frmlayout, mFade);
        }
                frmlayout.removeAllViews();
                frmlayout.addView(battle,0);

                final ImageView monster = battle.findViewById(R.id.monster_icon);
                final ImageView attack1View = battle.findViewById(R.id.myattack);
                final ImageView attack2View = battle.findViewById(R.id.theirattack);
                //add our different animations together to play
                AnimatorSet battleAnimation = new AnimatorSet();
                ArrayList<ObjectAnimator> batteset = new ArrayList<>();
                ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(
                        imageView, "translationX", 1.05f);
                scaleUpX.setDuration(1000L);


                ArrayList<Animator> attackanimations = new ArrayList<Animator>();
                final int player1 = R.drawable.baby_basic_1;
                final int player2 = R.drawable.baby_dino_2;

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
                            playerattack.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
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
                                }
                            });
                            player2attack.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
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
                                }
                            });
                            attackanimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
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
                                }
                            });


                            break;
                        case 1: //p1 win
                            playerattack.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
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
                                }
                            });
                            player2attack.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    monster.setVisibility(View.VISIBLE);
                                    monster.setScaleX(-1);
                                    final float progress = (float) animation.getAnimatedValue();
                                    monster.setBackgroundResource(player2);
                                    attack1View.setVisibility(View.INVISIBLE);
                                    attack2View.setVisibility(View.INVISIBLE);
                                }
                            });
                            attackanimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    monster.setVisibility(View.INVISIBLE);
                                    attack1View.setVisibility(View.VISIBLE);
                                    attack2View.setVisibility(View.INVISIBLE);
                                    final float progress = (float) animation.getAnimatedValue();
                                    float width = battle.getWidth()*progress;
                                    attack1View.setTranslationX(-width);
                                    attack2View.setTranslationX(width);
                                }
                            });
                            damaged.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
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
                                }
                            });
                            damagedanimation = true;
                            break;
                        case 2: //p2 win
                            playerattack.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    monster.setVisibility(View.VISIBLE);
                                    monster.setScaleX(1);
                                    monster.setBackgroundResource(player1);
                                    attack1View.setVisibility(View.INVISIBLE);
                                    attack2View.setVisibility(View.INVISIBLE);
                                }
                            });
                            player2attack.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
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
                            damaged.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
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
                        frmlayout.removeAllViews();
                        LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                        final View home = aboutinflater.inflate(R.layout.home_screen, (ViewGroup)null);
                        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH){
                            Fade mFade = new Fade(Fade.IN);
                            TransitionManager.beginDelayedTransition(frmlayout, mFade);
                        }
                        frmlayout.addView(home);
                        home.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {

                                selectedIcon(currentarrayid);

                                home.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }
                        });

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

    private class AsyncResume extends AsyncTask<String,TextView,String>{

        private int eventtype;
        private long stepsneeded;
        private boolean isEventreached = false;
        private String message;
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            String event = "Steps needed: ";
            Journey temp = db.journeyDao().getJourney().get(0);
            // currentevent = temp.getEventtype();
            stepsneeded = temp.getEventsteps();
            isEventreached = temp.isEventreached();
            message = event + stepsneeded;
            eventtype = temp.getEventtype();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            TvSteps.setText(message);
            // execution of result of Long time consuming operation
            if(stepsneeded <= 0 && isEventreached) {
                startEvent(eventtype);
            }
        }
    }

}
