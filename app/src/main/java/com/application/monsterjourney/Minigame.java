package com.application.monsterjourney;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ClipDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.StyleableRes;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Random;

public class Minigame extends AppCompatActivity {

    private int selectedgame;
    private AppDatabase db;
    private int currentarrayid;

    private ClipDrawable hungerfill;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_minigame);

        selectedgame = 0;

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
                        db = AppDatabase.buildDatabase(getApplicationContext());
                        currentarrayid = db.journeyDao().getMonster().get(0).getArrayid();
                        selectedIcon(currentarrayid);
                    }
                });
                homelayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        final FrameLayout frmlayout = (FrameLayout) findViewById(R.id.placeholder2);
        LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final View home = aboutinflater.inflate(R.layout.minigame_info, (ViewGroup)null);
        frmlayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                frmlayout.addView(home,0);
                prepareInfoView();
                frmlayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        ImageView rightscroll = findViewById(R.id.right_arrow);
        ImageView leftscroll = findViewById(R.id.left_arrow);
        Animation leftscrollanimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.leftarrow);

        Animation rightscrollanimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rightarrow);
        leftscroll.startAnimation(rightscrollanimation);
        rightscroll.startAnimation(leftscrollanimation);


        findViewById(R.id.playagain).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startGame();
                Minigame.StartMinigame runner = new Minigame.StartMinigame();
                runner.execute();
            }
        });

        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.playagain).setVisibility(View.INVISIBLE);
                findViewById(R.id.cancel).setVisibility(View.INVISIBLE);
                final FrameLayout frmlayout = (FrameLayout) findViewById(R.id.placeholder2);
                LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                final View home = aboutinflater.inflate(R.layout.minigame_info, (ViewGroup)null);
                frmlayout.removeAllViews();
                frmlayout.addView(home,0);
                prepareInfoView();
            }
        });


        ImageView temp = findViewById(R.id.hungerfill);
        hungerfill = (ClipDrawable) temp.getDrawable();
        Minigame.HungerFill runner = new Minigame.HungerFill();
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

    /**
     * prepares the main view for selecting the minigame to play
     */
    private void prepareInfoView(){

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Minigame.this, MainActivity.class);
                startActivity(intent);
                //where right side is current view
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
        });

        findViewById(R.id.confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startGame();
                Minigame.StartMinigame runner = new Minigame.StartMinigame();
                runner.execute();
            }
        });

        TextView titletext = findViewById(R.id.Title);
        TextView descriptiontext = findViewById(R.id.content);

        switch (selectedgame){
            case 0:
                titletext.setText(getText(R.string.simonsays_title));
                descriptiontext.setText(getText(R.string.simonsays_description));
                break;
        }

    }


    private void startGame(){
        findViewById(R.id.playagain).setVisibility(View.INVISIBLE);
        findViewById(R.id.cancel).setVisibility(View.INVISIBLE);
        final FrameLayout frmlayout = (FrameLayout) findViewById(R.id.placeholder2);
        LayoutInflater gameinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        switch(selectedgame){
            case 0:
                final View game = gameinflater.inflate(R.layout.simon_says, (ViewGroup)null);
                frmlayout.removeAllViews();
                frmlayout.addView(game,0);
                frmlayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {

                        simonSays(new ArrayList<Integer>());

                        frmlayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
                break;
        }

    }

    /**
     * our simon says game animation
     */
    private void simonSays(final ArrayList<Integer> rounds){
        final ImageView[] buttons = {findViewById(R.id.button1),findViewById(R.id.button2),findViewById(R.id.button3),findViewById(R.id.button4),
                findViewById(R.id.button5),findViewById(R.id.button6),findViewById(R.id.button7),findViewById(R.id.button8), findViewById(R.id.button9)};

        int rannum = new Random().nextInt(9);

        rounds.add(rannum);
        TextView temptext =  findViewById(R.id.count);
        String currentround = "Current Round: " + rounds.size();
        temptext.setText(currentround);
        ArrayList<Animator> gameanimations = new ArrayList<Animator>();
        for(final Integer round : rounds){
            ValueAnimator gamebutton = ValueAnimator.ofFloat(0.0f,1.0f);
            gamebutton.setInterpolator(new LinearInterpolator());
            gamebutton.setDuration(1000L);
            gamebutton.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    final float progress = (float) animation.getAnimatedValue();
                    if(progress < 0.1f){
                        for(int i = 0; i < 9 ; i++){
                            //buttons[i].setBackground(R.color.simonDefault);
                            buttons[i].setBackgroundResource(R.drawable.ic_simon_says_default);
                        }
                    }
                    else{
                            //buttons[round].setText(String.valueOf(1));
                        buttons[round].setBackgroundResource(R.drawable.ic_simon_says_blue);
                    }
                }
            });
            gameanimations.add(gamebutton);
        }
        AnimatorSet gameSet = new AnimatorSet();
        gameSet.playSequentially(gameanimations);
        gameSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                for(int i = 0; i < 9 ; i++){
                    //buttons[i].setText(String.valueOf(0));
                    buttons[i].setBackgroundResource(R.drawable.ic_simon_says_default);
                }
                attemptsimonSays(rounds, 0);

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        gameSet.start();
    }

    /**
     * attempt the simon says game
     */
    public void attemptsimonSays(final ArrayList<Integer> rounds, final int step){
        final ImageView[] buttons = {findViewById(R.id.button1),findViewById(R.id.button2),findViewById(R.id.button3),findViewById(R.id.button4),
                findViewById(R.id.button5),findViewById(R.id.button6),findViewById(R.id.button7),findViewById(R.id.button8), findViewById(R.id.button9)};
        for(int i = 0; i < 9; i++){
            final int tempi = i;
            buttons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    buttons[tempi].setBackgroundResource(R.drawable.ic_simon_says_red);
                    endSimonSays(rounds.size());
                    ImageView[] buttons = {findViewById(R.id.button1),findViewById(R.id.button2),findViewById(R.id.button3),findViewById(R.id.button4),
                            findViewById(R.id.button5),findViewById(R.id.button6),findViewById(R.id.button7),findViewById(R.id.button8), findViewById(R.id.button9)};
                    for(int i = 0; i < 9; i++){
                        buttons[i].setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                            }
                        });
                    }
                }
            });
        }
        final int newstep = step + 1;
        final int temp = rounds.get(step);
        buttons[temp].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttons[temp].setBackgroundResource(R.drawable.ic_simon_says_blue);
                Handler h = new Handler();
                //Run a runnable after 200ms that will change the correct colour back to grey
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        buttons[temp].setBackgroundResource(R.drawable.ic_simon_says_default);
                    }
                }, 200);
                if(step == rounds.size()-1){
                    ImageView[] buttons = {findViewById(R.id.button1),findViewById(R.id.button2),findViewById(R.id.button3),findViewById(R.id.button4),
                            findViewById(R.id.button5),findViewById(R.id.button6),findViewById(R.id.button7),findViewById(R.id.button8), findViewById(R.id.button9)};
                    for(int i = 0; i < 9; i++){
                        buttons[i].setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                            }
                        });
                    }
                    simonSays(rounds);
                }else{
                    attemptsimonSays(rounds,newstep);
                }
            }
        });

    }

    /**
     * ends the simon says game
     * @param currentround the number of correct choices
     */
    public void endSimonSays(int currentround){
        TextView temp =  findViewById(R.id.count);
        final long stepsearned = (currentround-1)*10;
        String roundspassed = "Steps earned: " + stepsearned;
        temp.setText(roundspassed);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                db = AppDatabase.buildDatabase(getApplicationContext());
                Journey tempjourney = db.journeyDao().getJourney().get(0);
                tempjourney.addStepstoJourney(stepsearned);
                Monster tempmonster = db.journeyDao().getMonster().get(0);
                tempmonster.setEvolvesteps(tempmonster.getEvolvesteps() - stepsearned);
                db.journeyDao().update(tempjourney);
                db.journeyDao().updateMonster(tempmonster);
            }
        });

        findViewById(R.id.playagain).setVisibility(View.VISIBLE);
        findViewById(R.id.cancel).setVisibility(View.VISIBLE);
    }

    //async task to initialize hunger bar
    private class HungerFill extends AsyncTask<String,TextView,String>{

        private int hunger;
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            Monster temp = db.journeyDao().getMonster().get(0);
            hunger = temp.getHunger();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            //where max clip is 10000, hunger is in increments of 8
            hungerfill.setLevel(hunger*1250);
        }
    }

    //async task for starting minigames, lowers
    private class StartMinigame extends AsyncTask<String,TextView,String>{

        private int hunger;
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            Monster temp = db.journeyDao().getMonster().get(0);
            hunger = temp.getHunger();
            if(hunger > 0){
                hunger = hunger -1;
                temp.setHunger(hunger);
            }
            else if (temp.getHatched()){
                temp.setMistakes(temp.getMistakes()+1);
            }
            db.journeyDao().updateMonster(temp);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            //where max clip is 10000, hunger is in increments of 8
            startGame();
            hungerfill.setLevel(hunger*1250);
        }
    }
}
