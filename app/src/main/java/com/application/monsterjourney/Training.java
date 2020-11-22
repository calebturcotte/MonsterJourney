package com.application.monsterjourney;

import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ClipDrawable;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StyleableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
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
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);
        selectedtask = 0;
        maxtasks = 5;

//        AsyncTask.execute(new Runnable() {
//            @Override
//            public void run() {
//                AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
//                //Temp test
//                Item tempitem = new Item(1);
//                Item tempitem2 = new Item(2);
//                db.journeyDao().insertItem(tempitem);
//                db.journeyDao().insertItem(tempitem2);
//            }
//        });

        title = findViewById(R.id.Title);
        description = findViewById(R.id.content);

        ImageView rightscroll = findViewById(R.id.right_arrow);
        ImageView leftscroll = findViewById(R.id.left_arrow);
        Animation leftscrollanimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.leftarrow);

        Animation rightscrollanimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rightarrow);
        leftscroll.startAnimation(rightscrollanimation);
        rightscroll.startAnimation(leftscrollanimation);

        findViewById(R.id.right_arrow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if(!isplaying)soundPool.load(context, R.raw.pressdown, 1);
                selectedtask = (selectedtask+1)%(maxtasks);
                taskInfo();
            }
        });
        findViewById(R.id.left_arrow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if(!isplaying)soundPool.load(context, R.raw.pressdown, 1);
                selectedtask = (selectedtask+maxtasks-1)%maxtasks;
                taskInfo();
            }
        });

        taskInfo();

        //add our home screen with the current monster
        final FrameLayout homelayout = (FrameLayout) findViewById(R.id.placeholder);
        LayoutInflater homeinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final View monsterview = homeinflater.inflate(R.layout.home_screen, (ViewGroup)null);
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH){
            Fade mFade = new Fade(Fade.IN);
            TransitionManager.beginDelayedTransition(homelayout, mFade);
        }
        homelayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                homelayout.addView(monsterview,0);

                ImageView imageView = monsterview.findViewById(R.id.monster_icon);
                imageView.setBackgroundResource(R.drawable.egg_idle);
                AnimationDrawable monsteranimator = (AnimationDrawable) imageView.getBackground();
                monsteranimator.start();
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
                        int currentarrayid = db.journeyDao().getMonster().get(0).getArrayid();
                        selectedIcon(currentarrayid);
                    }
                });
                homelayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        MonsterStats runner = new MonsterStats();
        runner.execute();

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(Training.this, MainActivity.class);
//                startActivity(intent);
//                //where right side is current view
//                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
        });

        findViewById(R.id.picker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTask();
            }
        });
    }

    /**
     * shows the current monster and description
     */
    public void taskInfo(){
        int tasktitle = R.string.missing_title;
        int tasktext = R.string.missing_description;
        switch(selectedtask){
            case 0:
                tasktitle = R.string.Training;
                tasktext = R.string.Traininginfo;
                break;
            case 1:
                tasktitle = R.string.Feeding;
                tasktext = R.string.Feedinginfo;
                break;
            case 2:
                tasktitle = R.string.Evolve;
                tasktext = R.string.EvolveInfo;
                break;
            case 3:
                tasktitle = R.string.Retire;
                tasktext = R.string.Retireinfo;
                break;
            case 4:
                tasktitle = R.string.Matchmaking;
                tasktext = R.string.Matchmakinginfo;
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
        trainView = traininflater.inflate(R.layout.training_game, null);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        boolean focusable2 = true; // lets taps outside the popup also dismiss it
        final PopupWindow trainWindow = new PopupWindow(trainView, width2, height2, focusable2);
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

                        TrainingResult runner = new TrainingResult();
                        runner.execute();

                        Handler h = new Handler();
                        //Run a runnable after 100ms (after that time it is safe to remove the view)
                        h.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                trainWindow.dismiss();
                                trainView = null;
                                MonsterStats runner = new MonsterStats();
                                runner.execute();
                            }
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
     * popup to select food to feed monster with
     */
    private void feed(){
        if(feedView != null){
            return;
        }
        LayoutInflater confirminflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert confirminflater != null;
        feedView = confirminflater.inflate(R.layout.feeding_popup, null);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        boolean focusable2 = true; // lets taps outside the popup also dismiss it
        final PopupWindow feedWindow = new PopupWindow(feedView, width2, height2, focusable2);
        feedWindow.setOutsideTouchable(false);


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            feedWindow.setElevation(20);
        }
        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        feedWindow.setAnimationStyle(R.style.PopupAnimation);
        feedWindow.showAtLocation(findViewById(R.id.back), Gravity.CENTER, 0, 0);
        feedView.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                feedWindow.dismiss();
                feedView = null;
                feedamounts = null;
            }
        });

        feedtype = 0;

        feedamounts = new ArrayList<>();
        feedamounts.add(new ItemAmount(0,1000));



        feeddisplay();

        FetchItems runner = new FetchItems();
        runner.execute();



        feedView.findViewById(R.id.confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
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
                    }
                });
                final FrameLayout frmlayout = (FrameLayout) findViewById(R.id.placeholder);
                LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                final View feeding = aboutinflater.inflate(R.layout.feeding_screen, (ViewGroup)null);
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH){
                    Fade mFade = new Fade(Fade.IN);
                    TransitionManager.beginDelayedTransition(frmlayout, mFade);
                }
                frmlayout.removeAllViews();
                frmlayout.addView(feeding,0);
                feeding.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        FoodAnimation runner = new FoodAnimation();
                        runner.execute();
                    }
                });
                feedWindow.dismiss();
                feedView = null;
                feedamounts = null;
                MonsterStats runner = new MonsterStats();
                runner.execute();
            }
        });

    }

    /**
     * configures display for the food popup
     */
    private void feeddisplay(){
        //TODO add icons and animation for each different type of food
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
                break;
            case 2:
                title.setText(getText(R.string.FoodTitle3));
                description.setText(getText(R.string.FoodDescription3));
                foodimage.setBackgroundResource(R.drawable.ic_food_display_1);
                break;
            case 3:
                title.setText(getText(R.string.FoodTitle4));
                description.setText(getText(R.string.FoodDescription4));
                foodimage.setBackgroundResource(R.drawable.ic_food_display_1);
                break;
            case 4:
                title.setText(getText(R.string.FoodTitle5));
                description.setText(getText(R.string.FoodDescription5));
                foodimage.setBackgroundResource(R.drawable.ic_food_display_1);
                break;
        }
        if(feedtype != 0){
            String tempamount = "x " + String.valueOf(feedamounts.get(feedtype).getAmount());
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
        EvolveRunner runner = new EvolveRunner();
        runner.execute();
    }

    /**
     * ask if they want to retire the monster, or notify that eggs can't be retired
     */
    public void retireConfirm(){
        LayoutInflater confirminflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert confirminflater != null;
        View confirmView = confirminflater.inflate(R.layout.confirm_popup, null);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        boolean focusable2 = true; // lets taps outside the popup also dismiss it
        final PopupWindow confirmWindow = new PopupWindow(confirmView, width2, height2, focusable2);
        confirmWindow.setOutsideTouchable(false);


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            confirmWindow.setElevation(20);
        }
        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        confirmWindow.setAnimationStyle(R.style.PopupAnimation);
        confirmWindow.showAtLocation(findViewById(R.id.back), Gravity.CENTER, 0, 0);
        confirmView.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmWindow.dismiss();
            }
        });

        confirmView.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmWindow.dismiss();
            }
        });

        confirmView.findViewById(R.id.confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retire();
            }
        });

        TextView message = confirmView.findViewById(R.id.confimation_text);
        message.setText(getText(R.string.RetireConfirm));

    }

    /**
     * retire the monster
     */
    public void retire(){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
                Monster temp = db.journeyDao().getMonster().get(0);
                History temphistory = new History(temp.getGeneration(), temp.getArrayid(), temp.getName());
                db.journeyDao().insertHistory(temphistory);
            }
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
        matchView = confirminflater.inflate(R.layout.matchmaker_popup, null);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        boolean focusable2 = true; // lets taps outside the popup also dismiss it
        final PopupWindow matchWindow = new PopupWindow(matchView, width2, height2, focusable2);
        matchWindow.setOutsideTouchable(false);


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            matchWindow.setElevation(20);
        }
        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        matchWindow.setAnimationStyle(R.style.PopupAnimation);
        matchWindow.showAtLocation(findViewById(R.id.back), Gravity.CENTER, 0, 0);
        matchView.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                matchWindow.dismiss();
                matchView = null;
            }
        });

        ImageView tempmatchmaker = matchView.findViewById(R.id.matchmaker_popup_icon);

        tempmatchmaker.setBackgroundResource(R.drawable.matchmaker_idle);
        AnimationDrawable matchanimator = (AnimationDrawable) tempmatchmaker.getBackground();
        matchanimator.start();

        TextView matchtext = matchView.findViewById(R.id.confimation_text);
        matchtext.setText(getText(R.string.MatchmakerText));

        matchView.findViewById(R.id.confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
                        Journey tempjourney = db.journeyDao().getJourney().get(0);
                        tempjourney.setMatching(true);
                        tempjourney.setMatchmakersteps(1000);
                        db.journeyDao().update(tempjourney);
                        matchWindow.dismiss();
                        matchView = null;
                    }
                });
            }
        });
        matchView.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
                        Journey tempjourney = db.journeyDao().getJourney().get(0);
                        tempjourney.setMatching(false);
                        tempjourney.setMatchmakersteps(1000);
                        db.journeyDao().update(tempjourney);
                        matchWindow.dismiss();
                        matchView = null;
                    }
                });
            }
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
        imageView.setBackgroundResource(resource);

        AnimationDrawable monsteranimator = (AnimationDrawable) imageView.getBackground();
        monsteranimator.start();

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
    private class TrainingResult extends AsyncTask<String,TextView,String>{

        private boolean trainresult;
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            Monster temp = db.journeyDao().getMonster().get(0);
            if (trainingtapcount >= 5){
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
            TextView trainingresult = trainView.findViewById(R.id.training_result);
            ImageView tempimage = trainView.findViewById(R.id.training_layout);
            tempimage.setVisibility(View.INVISIBLE);
            if(trainresult){
                trainingresult.setText(getText(R.string.TrainingSuccess));
            } else {
                trainingresult.setText(getText(R.string.TrainingFailed));
            }

        }
    }

    //fetch and display the matchmaking info for current status
    private class MatchmakingResult extends AsyncTask<String,TextView,String>{

        private int stage;
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            Monster temp = db.journeyDao().getMonster().get(0);
            int arrayid = temp.getArrayid();
            TypedArray array = getApplicationContext().getResources().obtainTypedArray(arrayid);
            int[] monsterresources = getApplicationContext().getResources().getIntArray(arrayid);
            stage = monsterresources[0];
            array.recycle();

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            //where max clip is 10000, hunger/diligence is in increments of 8
            TextView matchtext = matchView.findViewById(R.id.confimation_text);

            if(stage == 3){
                matchtext.setText(getText(R.string.MatchmakerText));
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
    private class FoodAnimation extends AsyncTask<String,String, String>{
        private int currentarrayid;
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            currentarrayid = db.journeyDao().getMonster().get(0).getArrayid();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            selectedIcon(currentarrayid);
            final ImageView foodimageView = findViewById(R.id.eating_icon);
            switch (feedtype){
                case 0:
                    foodimageView.setBackgroundResource(R.drawable.food_eat);
                    AnimationDrawable foodanimator = (AnimationDrawable) foodimageView.getBackground();
                    foodanimator.start();
                    break;
            }

            Handler h = new Handler();
            //Run a runnable to hide food after it has been eaten
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    foodimageView.setVisibility(View.INVISIBLE);
                }
            }, 1199);

        }
    }

    //perform check for evolving our monster and change view if appropriate
    private class EvolveRunner extends AsyncTask<String,String, String>{
        private int currentarrayid;
        private boolean evolved;
        private int stage;
        private long stepsneeded;
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            Monster temp = db.journeyDao().getMonster().get(0);
            evolved = temp.evolve(getApplicationContext());
            currentarrayid = db.journeyDao().getMonster().get(0).getArrayid();
            int[] monsterresources = getApplication().getResources().getIntArray(currentarrayid);
            stage = monsterresources[0];
            stepsneeded = temp.getEvolvesteps();

            db.journeyDao().updateMonster(temp);

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
            if(stage >= 3){
                Toast.makeText(getApplicationContext(), "Monster already at final stage.", Toast.LENGTH_SHORT).show();
            }
            else if(evolved){
                selectedIcon(currentarrayid);
            }
            else if(stepsneeded > 0){
                Toast.makeText(getApplicationContext(), String.valueOf(stepsneeded) + " steps needed to evolve.", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(getApplicationContext(), "Evolution requirements not met.", Toast.LENGTH_SHORT).show();
            }

            selectedIcon(currentarrayid);

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
            rightscroll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    feedtype = (feedtype +1)%5;
                    while(!feedamounts.get(feedtype).isUnlocked()){
                        feedtype = (feedtype +1)%5;
                    }
                    feeddisplay();
                }
            });

            leftscroll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    feedtype = (feedtype +4)%5;
                    while(!feedamounts.get(feedtype).isUnlocked()){
                        feedtype = (feedtype +4)%5;
                    }
                    feeddisplay();
                }
            });


        }
    }
}
