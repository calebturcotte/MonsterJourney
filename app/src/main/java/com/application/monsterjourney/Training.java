package com.application.monsterjourney;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.StyleableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

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
    private View trainView;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);
        selectedtask = 0;
        maxtasks = 4;

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
                tasktitle = R.string.Retire;
                tasktext = R.string.Retireinfo;
                break;
            case 3:
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
//                tasktitle = R.string.Training;
//                tasktext = R.string.Traininginfo;
                break;
            case 1:
                /*tasktitle = R.string.Feeding;
                tasktext = R.string.Feedinginfo;*/
                break;
            case 2:
//                tasktitle = R.string.Retire;
//                tasktext = R.string.Retireinfo;
                retireConfirm();

                break;
            case 3:
//                tasktitle = R.string.Matchmaking;
//                tasktext = R.string.Matchmakinginfo;
                break;
        }
    }

    /**
     * Starts the training activity
     * Tap spots as they pop up to fill up a bar
     */
    public void train(){
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

//                        AsyncTask.execute(new Runnable() {
//                            @Override
//                            public void run() {
//                                AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
//                                Monster temp = db.journeyDao().getMonster().get(0);
//                                if (trainingtapcount == 5){
//                                    if(temp.getDiligence() < 8){
//                                        temp.setDiligence(temp.getDiligence()+1);
//                                    }
//                                    //temp.trainingSuccess();
//                                }else{
//                                    temp.setMistakes(temp.getMistakes()+1);
//                                    //temp.trainingFail();
//                                }
//                                db.journeyDao().updateMonster(temp);
//                            }
//                        });

                        Handler h = new Handler();
                        //Run a runnable after 100ms (after that time it is safe to remove the view)
                        h.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                trainWindow.dismiss();
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
        temptouch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                temptouch.setVisibility(View.INVISIBLE);
                ImageView temp2 = trainView.findViewById(R.id.training_bar);
                ClipDrawable trainingfill = (ClipDrawable) temp2.getDrawable();
                //max fill is 10000, or tapcount of 5
                trainingtapcount = trainingtapcount + 1;
                trainingfill.setLevel(trainingtapcount*2000);
                startTrain(trainView);
            }
        });

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
            if (trainingtapcount == 5){
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
}
