package com.application.monsterjourney;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ClipDrawable;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StyleableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class Training extends AppCompatActivity {
    /**
     * all of our care tasks are done in this activity, training, feeding, retirement
     */
    private int selectedtask;
    private int maxtasks;
    private TextView title,description;
    private int trainingtapcount;
    private View trainView, matchView, feedView;
    private int feedtype;
    private ArrayList<ItemAmount> feedamounts;
    private MediaPlayer music;
    private boolean isplaying;
    private int currentarrayid;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);
        selectedtask = 0;
        maxtasks = 5;

        title = findViewById(R.id.Title);
        description = findViewById(R.id.content);

        ImageView rightscroll = findViewById(R.id.right_arrow);
        ImageView leftscroll = findViewById(R.id.left_arrow);
        Animation leftscrollanimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.leftarrow);

        Animation rightscrollanimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rightarrow);
        leftscroll.startAnimation(rightscrollanimation);
        rightscroll.startAnimation(leftscrollanimation);

        findViewById(R.id.right_arrow).setOnClickListener(v -> {
            //if(!isplaying)soundPool.load(context, R.raw.pressdown, 1);
            selectedtask = (selectedtask+1)%(maxtasks);
            taskInfo();
        });
        findViewById(R.id.left_arrow).setOnClickListener(v -> {
            //if(!isplaying)soundPool.load(context, R.raw.pressdown, 1);
            selectedtask = (selectedtask+maxtasks-1)%maxtasks;
            taskInfo();
        });

        findViewById(R.id.ranch_popup).setOnClickListener(v->{
            //music.release();
            Intent intent = new Intent(this, Ranch.class);
            startActivity(intent);
            //where right side is current view
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            music.release();
            finish();
        });

        taskInfo();

        //add our home screen with the current monster
        final FrameLayout homelayout = findViewById(R.id.placeholder);
        LayoutInflater homeinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        assert homeinflater != null;
        final View monsterview = homeinflater.inflate(R.layout.home_screen, (ViewGroup)null);
        Fade mFade = new Fade(Fade.IN);
        TransitionManager.beginDelayedTransition(homelayout, mFade);
        Activity myactivity = this;
        homelayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                homelayout.addView(monsterview,0);
                InitializeScreen runner = new InitializeScreen(myactivity);
                runner.execute();

                homelayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        MonsterStats runner = new MonsterStats();
        runner.execute();

        findViewById(R.id.back).setOnClickListener(v -> {
            music.release();
            finish();
        });

        findViewById(R.id.picker).setOnClickListener(v -> startTask());
    }

    @Override
    protected void onResume() {
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        int tempvolume = 80;
        music = MediaPlayer.create(Training.this,R.raw.training);
        music.setLooping(true);
        int currentvolume = settings.getInt("bgvolume", tempvolume);

        music.setVolume((float) currentvolume /100, (float) currentvolume /100);
        isplaying = settings.getBoolean("isplaying",isplaying);
        //music.prepareAsync();

        if(!isplaying)music.start();
        super.onResume();
    }

    @Override
    protected void onPause() {
        music.release();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * shows the current monster and description
     */
    public void taskInfo(){
        ImageView selector1 =  findViewById(R.id.selector1);
        ImageView selector2 =  findViewById(R.id.selector2);
        ImageView selector3 =  findViewById(R.id.selector3);
        ImageView selector4 =  findViewById(R.id.selector4);
        ImageView selector5 =  findViewById(R.id.selector5);
        selector1.setImageResource(R.drawable.ic_selection_off);
        selector2.setImageResource(R.drawable.ic_selection_off);
        selector3.setImageResource(R.drawable.ic_selection_off);
        selector4.setImageResource(R.drawable.ic_selection_off);
        selector5.setImageResource(R.drawable.ic_selection_off);

        int tasktitle = R.string.missing_title;
        int tasktext = R.string.missing_description;
        switch(selectedtask){
            case 0:
                tasktitle = R.string.Training;
                tasktext = R.string.Traininginfo;
                selector1.setImageResource(R.drawable.ic_selection_on);
                break;
            case 1:
                tasktitle = R.string.Feeding;
                tasktext = R.string.Feedinginfo;
                selector2.setImageResource(R.drawable.ic_selection_on);
                break;
            case 2:
                tasktitle = R.string.Evolve;
                tasktext = R.string.EvolveInfo;
                selector3.setImageResource(R.drawable.ic_selection_on);
                break;
            case 3:
                tasktitle = R.string.Retire;
                tasktext = R.string.Retireinfo;
                selector4.setImageResource(R.drawable.ic_selection_on);
                break;
            case 4:
                tasktitle = R.string.Matchmaking;
                tasktext = R.string.Matchmakinginfo;
                selector5.setImageResource(R.drawable.ic_selection_on);
                break;
        }

        title.setText(getText(tasktitle));
        description.setText(getText(tasktext));
    }

    /**
     * confirm selected care task
     */
    public void startTask(){
        switch(selectedtask){
            case 0:
                train();
                break;
            case 1:
                feed();
                break;
            case 2:
                evolve();
                break;
            case 3:
                retireConfirm();
                break;
            case 4:
                matchmake();
                break;
        }
    }

    /**
     * Starts the training activity
     * Tap spots as they pop up to fill up a bar
     */
    public void train(){
        if(trainView != null){
            return;
        }
        LayoutInflater traininflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert traininflater != null;
        trainView = traininflater.inflate(R.layout.training_game,findViewById(R.id.parent), false);
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
        trainWindow.showAtLocation(findViewById(R.id.back), Gravity.CENTER, 0, 0);
        final TextView trainingtitle = trainView.findViewById(R.id.training_title);
        final TextView trainingtime = trainView.findViewById(R.id.training_time);
        trainingtitle.setText(getText(R.string.TrainingReady));
        trainingtapcount = 0;
        Handler h = new Handler();
        Activity mainActivity = this;
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
                    TrainingResult runner = new TrainingResult(mainActivity);
                    runner.execute();

                    Handler h1 = new Handler();
                    //Run a runnable after 100ms (after that time it is safe to remove the view)
                    h1.postDelayed(() -> {
                        trainWindow.dismiss();
                        trainView = null;
                        MonsterStats runner1 = new MonsterStats();
                        runner1.execute();
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
     * fetch the number of successful taps
     * @return the number of taps for training
     */
    public int getTrainingtapcount(){
        return trainingtapcount;
    }

    /**
     * popup to select food to feed monster with
     */
    private void feed(){
        if(feedView != null){
            return;
        }
        LayoutInflater confirminflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert confirminflater != null;
        feedView = confirminflater.inflate(R.layout.feeding_popup,findViewById(R.id.parent), false);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow feedWindow = new PopupWindow(feedView, width2, height2, true);
        feedWindow.setOutsideTouchable(false);


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            feedWindow.setElevation(20);
        }
        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        feedWindow.setAnimationStyle(R.style.PopupAnimation);
        feedWindow.showAtLocation(findViewById(R.id.back), Gravity.CENTER, 0, 0);
        feedView.findViewById(R.id.close).setOnClickListener(v -> feedWindow.dismiss());

        feedWindow.setOnDismissListener(()->{
            feedView = null;
            feedamounts = null;
        });

        feedtype = 0;

        feedamounts = new ArrayList<>();
        feedamounts.add(new ItemAmount(0,1000));



        feeddisplay();

        FetchItems runner = new FetchItems();
        runner.execute();

        feedView.findViewById(R.id.confirm).setOnClickListener(v -> {
            AsyncTask.execute(() -> {
                AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
                Monster temp = db.journeyDao().getMonster().get(0);
                temp.feedMonster(feedtype);
                db.journeyDao().updateMonster(temp);

                List<Item> items = db.journeyDao().getItems();

                for(Item item : items){
                    if(item.getitem() == feedtype){
                        db.journeyDao().delete(item);
                        break;
                    }
                }
            });
            final FrameLayout frmlayout = findViewById(R.id.placeholder);
            LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            assert aboutinflater != null;
            final View feeding = aboutinflater.inflate(R.layout.feeding_screen, (ViewGroup)null);
            frmlayout.removeAllViews();
            frmlayout.addView(feeding,0);
            Activity myActivity = this;
            feeding.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    selectedIcon(currentarrayid);
                    FoodAnimation runner1 = new FoodAnimation(myActivity);
                    runner1.execute();
                    feeding.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
            feedWindow.dismiss();
            feedView = null;
            feedamounts = null;
            MonsterStats runner12 = new MonsterStats();
            runner12.execute();
        });

    }

    /**
     * configures display for the food popup
     */
    private void feeddisplay(){
        TextView title = feedView.findViewById(R.id.food_title);
        TextView description = feedView.findViewById(R.id.confimation_text);
        ImageView foodimage = feedView.findViewById(R.id.food_icon);
        TextView amount = feedView.findViewById(R.id.food_amount);
        switch(feedtype){
            case 0:
                title.setText(getText(R.string.FoodTitle1));
                description.setText(getText(R.string.FoodDescription1));
                foodimage.setBackgroundResource(R.drawable.ic_food_display_1);
                break;
            case 1:
                title.setText(getText(R.string.FoodTitle2));
                description.setText(getText(R.string.FoodDescription2));
                foodimage.setBackgroundResource(R.drawable.ic_food_display_0);
                break;
            case 2:
                title.setText(getText(R.string.FoodTitle3));
                description.setText(getText(R.string.FoodDescription3));
                foodimage.setBackgroundResource(R.drawable.ic_food_display_2);
                break;
            case 3:
                title.setText(getText(R.string.FoodTitle4));
                description.setText(getText(R.string.FoodDescription4));
                foodimage.setBackgroundResource(R.drawable.ic_food_display_3);
                break;
            case 4:
                title.setText(getText(R.string.FoodTitle5));
                description.setText(getText(R.string.FoodDescription5));
                foodimage.setBackgroundResource(R.drawable.ic_food_display_4);
                break;
        }
        if(feedtype != 0){
            String tempamount = "x " + feedamounts.get(feedtype).getAmount();
            amount.setText(tempamount);
        }
        else{
            amount.setText("");
        }
    }

    /**
     * evolve the monster if conditions are met, or display notification that monster cannot evolve
     */
    private void evolve(){
        EvolveRunner runner = new EvolveRunner(this);
        runner.execute();
    }

    /**
     * ask if they want to retire the monster, or notify that eggs can't be retired
     */
    public void retireConfirm(){
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
        confirmWindow.showAtLocation(findViewById(R.id.back), Gravity.CENTER, 0, 0);
        confirmView.findViewById(R.id.close).setOnClickListener(v -> confirmWindow.dismiss());

        confirmView.findViewById(R.id.back).setOnClickListener(v -> confirmWindow.dismiss());

        confirmView.findViewById(R.id.confirm).setOnClickListener(v ->{
            confirmWindow.dismiss();
            retire();
        } );

        TextView message = confirmView.findViewById(R.id.confimation_text);
        message.setText(getText(R.string.RetireConfirm));

    }

    /**
     * retire the monster
     */
    public void retire(){
        AsyncTask.execute(() -> {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            Monster temp = db.journeyDao().getMonster().get(0);
            History temphistory = new History(temp.getGeneration(), temp.getArrayid(), temp.getName());
            db.journeyDao().insertHistory(temphistory);
        });
        //if(!isplaying)music.pause();
        Intent intent = new Intent(Training.this, EggSelect.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        //where right side is current view
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    /**
     * start our matchmaking process if not already started
     */
    private void matchmake(){
        if(matchView != null){
            return;
        }
        LayoutInflater confirminflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert confirminflater != null;
        matchView = confirminflater.inflate(R.layout.matchmaker_popup,findViewById(R.id.parent), false);
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
        matchWindow.showAtLocation(findViewById(R.id.back), Gravity.CENTER, 0, 0);
        matchView.findViewById(R.id.close).setOnClickListener(v -> {
            matchWindow.dismiss();
            matchView = null;
        });

        ImageView tempmatchmaker = matchView.findViewById(R.id.matchmaker_popup_icon);

        tempmatchmaker.setBackgroundResource(R.drawable.matchmaker_idle);
        AnimationDrawable matchanimator = (AnimationDrawable) tempmatchmaker.getBackground();
        matchanimator.start();

        TextView matchtext = matchView.findViewById(R.id.confimation_text);
        matchtext.setText(getText(R.string.MatchmakerText));

        matchView.findViewById(R.id.confirm).setOnClickListener(v -> {
            AsyncTask.execute(() -> {
                AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
                Journey tempjourney = db.journeyDao().getJourney().get(0);
                tempjourney.setMatching(true);
                tempjourney.setMatchmakersteps(2000);
                db.journeyDao().update(tempjourney);
            });
            matchWindow.dismiss();
            matchView = null;
        });
        matchView.findViewById(R.id.back).setOnClickListener(v -> {
            AsyncTask.execute(() -> {
                AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
                Journey tempjourney = db.journeyDao().getJourney().get(0);
                tempjourney.setMatching(false);
                tempjourney.setMatchmakersteps(2000);
                db.journeyDao().update(tempjourney);
            });
            matchWindow.dismiss();
            matchView = null;
        });
        matchView.findViewById(R.id.confirm).setVisibility(View.INVISIBLE);
        matchView.findViewById(R.id.back).setVisibility(View.INVISIBLE);

        MatchmakingResult runner = new MatchmakingResult();
        runner.execute();

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
        ImageView imageView = findViewById(R.id.monster_icon);
        imageView.setImageDrawable(ContextCompat.getDrawable(this, resource));
        imageView.setBackgroundResource(R.drawable.background_training);

        AnimationDrawable monsteranimator = (AnimationDrawable) imageView.getDrawable();
        monsteranimator.start();

    }

    /**
     * play happy animation for selected amount of time
     */
    public void happyAnimation(int duration){
        TypedArray array = getBaseContext().getResources().obtainTypedArray(currentarrayid);
        @StyleableRes int index = 4;
        int[] monsterresources = getResources().getIntArray(currentarrayid);
        int evolutions = monsterresources[1];
        int resource = array.getResourceId(index+evolutions+10, R.drawable.egg_idle);
        array.recycle();
        ImageView imageView = findViewById(R.id.monster_icon);
        imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), resource));
        AnimationDrawable monsteranimator = (AnimationDrawable) imageView.getDrawable();
        monsteranimator.start();
        Handler h = new Handler();
        h.postDelayed(() -> {
            monsteranimator.stop();
            selectedIcon(currentarrayid);
        }, duration);
    }

    /**
     * play angry animation for selected amount of time
     */
    public void angryAnimation(int duration){
        TypedArray array = getBaseContext().getResources().obtainTypedArray(currentarrayid);
        @StyleableRes int index = 4;
        int[] monsterresources = getResources().getIntArray(currentarrayid);
        int evolutions = monsterresources[1];
        int resource = array.getResourceId(index+evolutions+9, R.drawable.egg_idle);
        array.recycle();
        ImageView imageView = findViewById(R.id.monster_icon);
        imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), resource));
        AnimationDrawable monsteranimator = (AnimationDrawable) imageView.getDrawable();
        monsteranimator.start();
        Handler h = new Handler();
        h.postDelayed(() -> {
            monsteranimator.stop();
            selectedIcon(currentarrayid);
        }, duration);
    }


    //fetch and display the monster info for current status
    private class MonsterStats extends AsyncTask<String,TextView,String>{

        private int hunger;
        private int training;
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            Monster temp = db.journeyDao().getMonster().get(0);
            hunger = temp.getHunger();
            training = temp.getDiligence();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            //where max clip is 10000, hunger/diligence is in increments of 8
            ImageView temp = findViewById(R.id.hungerfill);
            ClipDrawable hungerfill = (ClipDrawable) temp.getDrawable();
            ImageView temp2 = findViewById(R.id.trainingfill);

            ClipDrawable trainingfill = (ClipDrawable) temp2.getDrawable();
            hungerfill.setLevel(hunger*1250);
            trainingfill.setLevel(training*1250);

        }
    }

    //fetch and display the monster info for current status
    private static class TrainingResult extends AsyncTask<String,TextView,String>{

        private boolean trainresult;
        private final WeakReference<Activity> weakActivity;

        public TrainingResult(Activity myActivity){
            this.weakActivity = new WeakReference<>(myActivity);
        }
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.getInstance(weakActivity.get());
            Training training = (Training) weakActivity.get();
            Monster temp = db.journeyDao().getMonster().get(0);
            if (training.getTrainingtapcount() >= 5){
                trainresult = true;
                if(temp.getDiligence() < 8){
                    temp.setDiligence(temp.getDiligence()+1);
                }
                //temp.trainingSuccess();
            }else{
                trainresult = false;
                temp.setMistakes(temp.getMistakes()+1);
                //temp.trainingFail();
            }
            db.journeyDao().updateMonster(temp);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            //where max clip is 10000, hunger/diligence is in increments of 8
            Training training = (Training) weakActivity.get();
            TextView trainingresult = training.trainView.findViewById(R.id.training_result);
            ImageView tempimage = training.trainView.findViewById(R.id.training_layout);
            tempimage.setVisibility(View.INVISIBLE);
            if(trainresult){
                training.happyAnimation(3000);
                trainingresult.setText(weakActivity.get().getText(R.string.TrainingSuccess));
            } else {
                training.angryAnimation(3000);
                trainingresult.setText(weakActivity.get().getText(R.string.TrainingFailed));
            }

        }
    }

    //fetch and display the matchmaking info for current status
    private class MatchmakingResult extends AsyncTask<String,TextView,String>{

        private int stage;
        boolean ismatching;
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            Monster temp = db.journeyDao().getMonster().get(0);
            int arrayid = temp.getArrayid();
            TypedArray array = getApplicationContext().getResources().obtainTypedArray(arrayid);
            int[] monsterresources = getApplicationContext().getResources().getIntArray(arrayid);
            stage = monsterresources[0];
            ismatching = db.journeyDao().getJourney().get(0).isMatching();
            array.recycle();

            return null;
        }

        @Override
        protected void onPostExecute(String result) {

            TextView matchtext = matchView.findViewById(R.id.confimation_text);

            if(stage == 3){
                if(ismatching){
                    matchtext.setText(getText(R.string.CurrentBreeding));
                }else{
                    matchtext.setText(getText(R.string.MatchmakerText));
                }
                matchView.findViewById(R.id.confirm).setVisibility(View.VISIBLE);
                matchView.findViewById(R.id.back).setVisibility(View.VISIBLE);
            } else {
                matchtext.setText(getText(R.string.MatchmakerEarly));
            }

        }
    }

    /**
     * animation upon food eaten
     */
    private static class FoodAnimation extends AsyncTask<String,String, String>{
        private int currentarrayid;
        private final WeakReference<Activity> weakActivity;

        public FoodAnimation(Activity myActivity){
            this.weakActivity = new WeakReference<>(myActivity);
        }
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(weakActivity.get());
            currentarrayid = db.journeyDao().getMonster().get(0).getArrayid();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Training thisActivity = (Training) weakActivity.get();
            thisActivity.selectedIcon(currentarrayid);
            FrameLayout homelayout = weakActivity.get().findViewById(R.id.placeholder);
            ImageView foodimageView = homelayout.findViewById(R.id.eating_icon);
            AnimationDrawable foodanimator;
            switch (thisActivity.feedtype){
                case 1: //large apple
                    foodimageView.setImageDrawable(ContextCompat.getDrawable(weakActivity.get(),R.drawable.food1_eat));
                    foodanimator = (AnimationDrawable) foodimageView.getDrawable();
                    foodanimator.start();
                    break;
                case 2: //meat
                    foodimageView.setImageDrawable(ContextCompat.getDrawable(weakActivity.get(),R.drawable.food2_eat));
                    foodanimator = (AnimationDrawable) foodimageView.getDrawable();
                    foodanimator.start();
                    break;
                case 3: //training pill
                    foodimageView.setImageDrawable(ContextCompat.getDrawable(weakActivity.get(),R.drawable.food3_eat));
                    foodanimator = (AnimationDrawable) foodimageView.getDrawable();
                    foodanimator.start();
                    break;
                case 4: //evolution medicine
                    foodimageView.setImageDrawable(ContextCompat.getDrawable(weakActivity.get(),R.drawable.food4_eat));
                    foodanimator = (AnimationDrawable) foodimageView.getDrawable();
                    foodanimator.start();
                    break;
                default: // basic apple
                    foodimageView.setImageDrawable(ContextCompat.getDrawable(weakActivity.get(),R.drawable.food_eat));
                    foodanimator = (AnimationDrawable) foodimageView.getDrawable();
                    foodanimator.start();
                    break;

            }

            Handler h = new Handler();
            //Run a runnable to hide food after it has been eaten
            h.postDelayed(() -> {
                foodanimator.stop();
                LayoutInflater homeinflater = (LayoutInflater) weakActivity.get().getSystemService(LAYOUT_INFLATER_SERVICE);
                assert homeinflater != null;
                final View monsterview = homeinflater.inflate(R.layout.home_screen, (ViewGroup)null);
                Fade mFade = new Fade(Fade.IN);
                TransitionManager.beginDelayedTransition(homelayout, mFade);
                homelayout.removeAllViews();
                homelayout.addView(monsterview,0);
                homelayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
//                        InitializeScreen runner = new InitializeScreen(weakActivity.get());
//                        runner.execute();
                        thisActivity.selectedIcon(currentarrayid);
                        thisActivity.happyAnimation(2000);

                        homelayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
            }, 1199);

        }
    }

    //perform check for evolving our monster and change view if appropriate
    private static class EvolveRunner extends AsyncTask<String,String, String>{
        private int currentarrayid;
        private boolean evolved;
        private int stage;
        private long stepsneeded;

        private final WeakReference<Activity> weakActivity;

        public EvolveRunner(Activity myActivity){
            this.weakActivity = new WeakReference<>(myActivity);
        }
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(weakActivity.get());
            Monster temp = db.journeyDao().getMonster().get(0);
            Journey tempjourney = db.journeyDao().getJourney().get(0);
            evolved = temp.evolve(weakActivity.get(), tempjourney.getEvolveddiscount());
            //currentarrayid = db.journeyDao().getMonster().get(0).getArrayid();
            currentarrayid = temp.getArrayid();
            int[] monsterresources = weakActivity.get().getResources().getIntArray(currentarrayid);
            stage = monsterresources[0];
            stepsneeded = temp.getEvolvesteps();

            db.journeyDao().updateMonster(temp);

            Training training = (Training) weakActivity.get();
            training.currentarrayid = currentarrayid;


            List<UnlockedMonster> unlockedMonsters = db.journeyDao().getUnlockedMonster();
            if(evolved){
                for(UnlockedMonster unlockedMonster : unlockedMonsters){
                    if(unlockedMonster.getMonsterarrayid() == currentarrayid){
                        unlockedMonster.setUnlocked(true);
                        unlockedMonster.setDiscovered(true);
                    }
                }
                db.journeyDao().updateUnlockedMonster(unlockedMonsters);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Training training = (Training) weakActivity.get();
            if(evolved){
                ImageView eventimage = training.findViewById(R.id.monster_event);
                eventimage.setVisibility(View.VISIBLE);
                final Animation eventanimation = new AlphaAnimation(1, 0); //to change visibility from visible to invisible
                eventanimation.setDuration(400); //half a second duration for each animation cycle
                eventanimation.setInterpolator(new LinearInterpolator());
                eventanimation.setRepeatCount(Animation.INFINITE); //repeating indefinitely
                eventanimation.setRepeatMode(Animation.REVERSE); //animation will start from end point once ended.
                eventimage.startAnimation(eventanimation); //to start animation
                Handler h1 = new Handler();
                //Run a runnable after 100ms (after that time it is safe to remove the view)
                h1.postDelayed(() -> {
                    eventimage.setVisibility(View.INVISIBLE);
                    eventanimation.cancel();
                    training.selectedIcon(currentarrayid);
                    training.happyAnimation(2000);
                }, 2000);

            }
            else if(stage >= 3){
                Toast.makeText(weakActivity.get(), "Monster already at final stage.", Toast.LENGTH_SHORT).show();
            }
            else if(stepsneeded > 0){
                Toast.makeText(weakActivity.get(), stepsneeded + " steps needed to evolve.", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(weakActivity.get(), "Evolution requirements not met.", Toast.LENGTH_SHORT).show();
            }
//            ImageView eventimage = findViewById(R.id.monster_event);
//            eventimage.setVisibility(View.VISIBLE);
//            final Animation eventanimation = new AlphaAnimation(1, 0); //to change visibility from visible to invisible
//            eventanimation.setDuration(400); //half a second duration for each animation cycle
//            eventanimation.setInterpolator(new LinearInterpolator());
//            eventanimation.setRepeatCount(Animation.INFINITE); //repeating indefinitely
//            eventanimation.setRepeatMode(Animation.REVERSE); //animation will start from end point once ended.
//            eventimage.startAnimation(eventanimation); //to start animation
//
//            selectedIcon(currentarrayid);

        }
    }

    //fetch and display the matchmaking info for current status
    private class FetchItems extends AsyncTask<String,TextView,String>{

        private int item1;
        private int item2;
        private int item3;
        private int item4;
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            List<Item> tempList = db.journeyDao().getItems();
            item1 = 0;
            item2 = 0;
            item3 = 0;
            item4 = 0;

            for(Item item : tempList){
                switch(item.getitem()){
                    case 1:
                        item1++;
                        break;
                    case 2:
                        item2++;
                        break;
                    case 3:
                        item3++;
                        break;
                    case 4:
                        item4++;
                        break;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            //check if item is owned, if so then it can be an option to view
            ImageView rightscroll = feedView.findViewById(R.id.right_popup_arrow);
            ImageView leftscroll = feedView.findViewById(R.id.left_popup_arrow);
            if(item1 > 0 || item2 > 0 || item3 > 0 || item4 > 0){
                rightscroll.setVisibility(View.VISIBLE);
                leftscroll.setVisibility(View.VISIBLE);

                Animation leftscrollanimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.leftarrow);

                Animation rightscrollanimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rightarrow);
                leftscroll.startAnimation(rightscrollanimation);
                rightscroll.startAnimation(leftscrollanimation);
            }

            feedamounts.add(new ItemAmount(1,item1));
            feedamounts.add(new ItemAmount(2,item2));
            feedamounts.add(new ItemAmount(3,item3));
            feedamounts.add(new ItemAmount(4,item4));
            rightscroll.setOnClickListener(v -> {
                feedtype = (feedtype +1)%5;
                while(feedamounts.get(feedtype).isUnlocked()){
                    feedtype = (feedtype +1)%5;
                }
                feeddisplay();
            });

            leftscroll.setOnClickListener(v -> {
                feedtype = (feedtype +4)%5;
                while(feedamounts.get(feedtype).isUnlocked()){
                    feedtype = (feedtype +4)%5;
                }
                feeddisplay();
            });
        }
    }

    //fetch and display the matchmaking info for current status
    private static class InitializeScreen extends AsyncTask<String,TextView,String>{

        private int currentarrayid;
        private final WeakReference<Activity> weakActivity;

        public InitializeScreen(Activity myActivity){
            this.weakActivity = new WeakReference<>(myActivity);
        }
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(weakActivity.get());
            currentarrayid = db.journeyDao().getMonster().get(0).getArrayid();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            //check if item is owned, if so then it can be an option to view
            Training training = (Training) weakActivity.get();
            training.currentarrayid = currentarrayid;

            training.selectedIcon(currentarrayid);

        }
    }

}
