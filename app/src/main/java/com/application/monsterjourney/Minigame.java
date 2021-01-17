package com.application.monsterjourney;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ClipDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.StyleableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Random;

public class Minigame extends AppCompatActivity {

    private int selectedgame;

    private int snakedirection;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_minigame);

        selectedgame = 0;
        int maxgames = 2;

        //add our home screen with the current monster
        final FrameLayout homelayout = findViewById(R.id.placeholder);
        LayoutInflater homeinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        assert homeinflater != null;
        final View monsterview = homeinflater.inflate(R.layout.home_screen, null);
        Fade mFade = new Fade(Fade.IN);
        TransitionManager.beginDelayedTransition(homelayout, mFade);
        homelayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                homelayout.addView(monsterview,0);
                homelayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        Activity activity = this;
        final FrameLayout frmlayout = findViewById(R.id.placeholder2);
        LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        assert aboutinflater != null;
        final View home = aboutinflater.inflate(R.layout.minigame_info, null);
        frmlayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                frmlayout.removeAllViews();
                frmlayout.addView(home,0);
                prepareInfoView(true);
                home.getViewTreeObserver().addOnGlobalLayoutListener(() -> {


                    ImageView rightscroll = findViewById(R.id.right_arrow);
                    ImageView leftscroll = findViewById(R.id.left_arrow);
//                    Animation leftscrollanimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.leftarrow);
//
//                    Animation rightscrollanimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rightarrow);
//                    leftscroll.startAnimation(rightscrollanimation);
//                    rightscroll.startAnimation(leftscrollanimation);

                    leftscroll.setOnClickListener(v -> {
                        selectedgame = (selectedgame + maxgames - 1)%maxgames;
                        prepareInfoView(false);
                    });

                    rightscroll.setOnClickListener(v -> {
                        selectedgame = (selectedgame + 1)%maxgames;
                        prepareInfoView(false);
                    });


                    findViewById(R.id.playagain).setOnClickListener(v -> {
                        StartMinigame runner = new StartMinigame(activity);
                        runner.execute();
                        startGame();
                    });

                    findViewById(R.id.cancel).setOnClickListener(v -> {
                        findViewById(R.id.playagain).setVisibility(View.INVISIBLE);
                        findViewById(R.id.cancel).setVisibility(View.INVISIBLE);
                        rightscroll.setVisibility(View.VISIBLE);
                        leftscroll.setVisibility(View.VISIBLE);
//                        leftscroll.startAnimation(rightscrollanimation);
//                        rightscroll.startAnimation(leftscrollanimation);
                        LayoutInflater aboutinflater1 = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                        assert aboutinflater1 != null;
                        final View home1 = aboutinflater1.inflate(R.layout.minigame_info, null);
                        frmlayout.removeAllViews();
                        frmlayout.addView(home1,0);
                        prepareInfoView(true);
                    });


                    Minigame.HungerFill runner = new Minigame.HungerFill(activity);
                    runner.execute();

                    home.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                });
                frmlayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });


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
        array.recycle();
        ImageView imageView = activity.findViewById(R.id.monster_icon);
        imageView.setBackgroundResource(R.drawable.ic_background_minigame);
        imageView.setImageDrawable(ContextCompat.getDrawable(activity.getApplicationContext(), resource));

        AnimationDrawable monsteranimator = (AnimationDrawable) imageView.getDrawable();
        monsteranimator.start();
    }

    /**
     * prepares the main view for selecting the minigame to play
     */
    private void prepareInfoView(boolean firstinitialize){
        if(firstinitialize){
            findViewById(R.id.back).setOnClickListener(v -> finish());

            findViewById(R.id.confirm).setOnClickListener(v -> {
                StartMinigame runner = new StartMinigame(this);
                runner.execute();
                startGame();
            });
        }

        ImageView selector1 =  findViewById(R.id.selector1);
        ImageView selector2 =  findViewById(R.id.selector2);
        selector1.setImageResource(R.drawable.ic_selection_off);
        selector2.setImageResource(R.drawable.ic_selection_off);


        TextView titletext = findViewById(R.id.Title);
        TextView descriptiontext = findViewById(R.id.content);

        switch (selectedgame){
            case 0:
                titletext.setText(getText(R.string.simonsays_title));
                descriptiontext.setText(getText(R.string.simonsays_description));
                selector1.setImageResource(R.drawable.ic_selection_on);
                break;
            case 1:
                titletext.setText(getText(R.string.snake_title));
                descriptiontext.setText(getText(R.string.snake_description));
                selector2.setImageResource(R.drawable.ic_selection_on);
                break;
        }

    }


    public void startGame(){
        //TODO doesn't turn invisible, monster animation stops when playing snake game
        //can maybe not use the animation
        findViewById(R.id.playagain).setVisibility(View.INVISIBLE);
        findViewById(R.id.cancel).setVisibility(View.INVISIBLE);
        ImageView rightscroll = findViewById(R.id.right_arrow);
        ImageView leftscroll = findViewById(R.id.left_arrow);
        rightscroll.clearAnimation();
        leftscroll.clearAnimation();
        rightscroll.setVisibility(View.INVISIBLE);
        leftscroll.setVisibility(View.INVISIBLE);
        final FrameLayout frmlayout = findViewById(R.id.placeholder2);
        LayoutInflater gameinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        switch(selectedgame){
            case 0:
                assert gameinflater != null;
                final View game = gameinflater.inflate(R.layout.simon_says, findViewById(R.id.parent), false);
                frmlayout.removeAllViews();
                frmlayout.addView(game,0);
                game.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        simonSays(new ArrayList<>());

                        game.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
                break;
            case 1:
                assert gameinflater != null;
                final View game2 = gameinflater.inflate(R.layout.chain_game, findViewById(R.id.parent), false);
                frmlayout.removeAllViews();
                frmlayout.addView(game2,0);
                frmlayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
//                        ImageView rightscroll = findViewById(R.id.right_arrow);
//                        ImageView leftscroll = findViewById(R.id.left_arrow);
//                        rightscroll.clearAnimation();
//                        leftscroll.clearAnimation();
//                        rightscroll.setVisibility(View.INVISIBLE);
//                        leftscroll.setVisibility(View.INVISIBLE);
//                        Handler h = new Handler();
//                        //Run a runnable after 200ms that will change the correct colour back to grey
//                        h.postDelayed(() -> snakeGame(), 200);
                        snakeGame();

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
        ArrayList<Animator> gameanimations = new ArrayList<>();
        for(final Integer round : rounds){
            ValueAnimator gamebutton = ValueAnimator.ofFloat(0.0f,1.0f);
            gamebutton.setInterpolator(new LinearInterpolator());
            gamebutton.setDuration(1000L);
            gamebutton.addUpdateListener(animation -> {
                final float progress = (float) animation.getAnimatedValue();
                if(progress < 0.1f){
                    for(int i = 0; i < 9 ; i++){
                        buttons[i].setBackgroundResource(R.drawable.ic_simon_says_default);
                    }
                }
                else{
                    buttons[round].setBackgroundResource(R.drawable.ic_simon_says_blue);
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
            buttons[i].setOnClickListener(v -> {
                buttons[tempi].setBackgroundResource(R.drawable.ic_simon_says_red);
                endSimonSays(rounds.size());
                for(int i1 = 0; i1 < 9; i1++){
                    buttons[i1].setOnClickListener(v1 -> {
                    });
                }
            });
        }
        final int newstep = step + 1;
        final int temp = rounds.get(step);
        buttons[temp].setOnClickListener(v -> {
            int handlerdelay = 200;
            if(step == rounds.size()-1){
                TextView temptext =  findViewById(R.id.count);
                temptext.setText(getText(R.string.simonsays_rounddone));
                handlerdelay = 1000;
            }else{
                attemptsimonSays(rounds,newstep);
            }
            buttons[temp].setBackgroundResource(R.drawable.ic_simon_says_blue);
            Handler h = new Handler();
            //Run a runnable after 200ms that will change the correct colour back to grey
            h.postDelayed(() -> {
                buttons[temp].setBackgroundResource(R.drawable.ic_simon_says_default);
                if(step == rounds.size()-1){
                    for(int i = 0; i < 9; i++){
                        buttons[i].setOnClickListener(v12 -> {
                        });
                    }
                    simonSays(rounds);
                }
            }, handlerdelay);
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

        AsyncTask.execute(() -> {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            Journey tempjourney = db.journeyDao().getJourney().get(0);
            Monster tempmonster = db.journeyDao().getMonster().get(0);
            tempjourney.addStepstoJourney(stepsearned, tempmonster.getHatched());
            tempmonster.setEvolvesteps(tempmonster.getEvolvesteps() - stepsearned);
            db.journeyDao().update(tempjourney);
            db.journeyDao().updateMonster(tempmonster);
        });

        findViewById(R.id.playagain).setVisibility(View.VISIBLE);
        findViewById(R.id.cancel).setVisibility(View.VISIBLE);
    }

    /**
     * start our snake game
     */
    public void snakeGame(){
        snakedirection = 0; //where 0 means not moving
        findViewById(R.id.up_direction).setOnClickListener(v -> snakedirection = 1);
        findViewById(R.id.left_direction).setOnClickListener(v -> snakedirection = 2);
        findViewById(R.id.down_direction).setOnClickListener(v -> snakedirection = 3);
        findViewById(R.id.right_direction).setOnClickListener(v -> snakedirection = 4);
        ArrayList<SnakePosition> snakePositions = new ArrayList<>();
        snakePositions.add(new SnakePosition(4,4,R.id.snake_head));
        addSnakeSection(snakePositions);
        snakeMove(snakePositions);

    }

    /**
     * move our snake along the view
     * @param snakePositions the container for each snake in the view
     */
    public void snakeMove(ArrayList<SnakePosition> snakePositions){
        ConstraintLayout templayout = findViewById(R.id.game_spot);
        TextView counttext = findViewById(R.id.count);
        String countstring = "Energy collected: " + (snakePositions.size() - 2);
        counttext.setText(countstring);
        for(SnakePosition snakePosition : snakePositions){
            int viewid = snakePosition.getViewid();
            int xposition = snakePosition.getXposition();
            int yposition = snakePosition.getYposition();

            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(templayout);
            switch(xposition){
                case 0:
                    constraintSet.connect(viewid,ConstraintSet.START,R.id.vguideline0,ConstraintSet.START,0);
                    break;
                case 1:
                    constraintSet.connect(viewid,ConstraintSet.START,R.id.vguideline1,ConstraintSet.START,0);
                    break;
                case 2:
                    constraintSet.connect(viewid,ConstraintSet.START,R.id.vguideline2,ConstraintSet.START,0);
                    break;
                case 3:
                    constraintSet.connect(viewid,ConstraintSet.START,R.id.vguideline3,ConstraintSet.START,0);
                    break;
                case 4:
                    constraintSet.connect(viewid,ConstraintSet.START,R.id.vguideline4,ConstraintSet.START,0);
                    break;
                case 5:
                    constraintSet.connect(viewid,ConstraintSet.START,R.id.vguideline5,ConstraintSet.START,0);
                    break;
                case 6:
                    constraintSet.connect(viewid,ConstraintSet.START,R.id.vguideline6,ConstraintSet.START,0);
                    break;
                case 7:
                    constraintSet.connect(viewid,ConstraintSet.START,R.id.vguideline7,ConstraintSet.START,0);
                    break;
                case 8:
                    constraintSet.connect(viewid,ConstraintSet.START,R.id.vguideline8,ConstraintSet.START,0);
                    break;
                case 9:
                    constraintSet.connect(viewid,ConstraintSet.START,R.id.vguideline9,ConstraintSet.START,0);
                    break;
            }

            switch(yposition){
                case 0:
                    constraintSet.connect(viewid,ConstraintSet.TOP,R.id.guideline0,ConstraintSet.TOP,0);
                    break;
                case 1:
                    constraintSet.connect(viewid,ConstraintSet.TOP,R.id.guideline1,ConstraintSet.TOP,0);
                    break;
                case 2:
                    constraintSet.connect(viewid,ConstraintSet.TOP,R.id.guideline2,ConstraintSet.TOP,0);
                    break;
                case 3:
                    constraintSet.connect(viewid,ConstraintSet.TOP,R.id.guideline3,ConstraintSet.TOP,0);
                    break;
                case 4:
                    constraintSet.connect(viewid,ConstraintSet.TOP,R.id.guideline4,ConstraintSet.TOP,0);
                    break;
                case 5:
                    constraintSet.connect(viewid,ConstraintSet.TOP,R.id.guideline5,ConstraintSet.TOP,0);
                    break;
                case 6:
                    constraintSet.connect(viewid,ConstraintSet.TOP,R.id.guideline6,ConstraintSet.TOP,0);
                    break;
                case 7:
                    constraintSet.connect(viewid,ConstraintSet.TOP,R.id.guideline7,ConstraintSet.TOP,0);
                    break;
                case 8:
                    constraintSet.connect(viewid,ConstraintSet.TOP,R.id.guideline8,ConstraintSet.TOP,0);
                    break;
                case 9:
                    constraintSet.connect(viewid,ConstraintSet.TOP,R.id.guideline9,ConstraintSet.TOP,0);
                    break;
            }
            constraintSet.constrainHeight(viewid, 0);
            constraintSet.constrainWidth(viewid, 0);
            constraintSet.constrainPercentHeight(viewid, 0.1f);
            constraintSet.constrainPercentWidth(viewid, 0.1f);
            constraintSet.applyTo(templayout);
        }

        int xposition = snakePositions.get(0).getXposition();
        int yposition = snakePositions.get(0).getYposition();


        switch (snakedirection){
            case 1: //up
                yposition = yposition - 1;
                if(yposition < 0) {
                    yposition = 0;
                    snakedirection = 0;
                }
                break;
            case 2: //left
                xposition = xposition - 1;
                if(xposition < 0) {
                    xposition = 0;
                    snakedirection = 0;
                }
                break;
            case 3: //down
                yposition = yposition + 1;
                if(yposition > 9){
                    yposition = 9;
                    snakedirection = 0;
                }
                break;
            case 4: //right
                xposition = xposition + 1;
                if(xposition > 9){
                    xposition = 9;
                    snakedirection = 0;
                }
                break;
        }

        if(xposition == snakePositions.get(snakePositions.size()-1).getXposition() && yposition == snakePositions.get(snakePositions.size()-1).getYposition()){
            if(snakePositions.size() < 10*10){
                addSnakeSection(snakePositions);
            }
            else {
                int stepsearned = (snakePositions.size() - 2)*7;
                countstring = "Steps Earned: " + stepsearned;
                counttext.setText(countstring);
                AsyncTask.execute(() -> {
                    AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
                    Journey tempjourney = db.journeyDao().getJourney().get(0);
                    Monster tempmonster = db.journeyDao().getMonster().get(0);
                    tempjourney.addStepstoJourney(stepsearned, tempmonster.getHatched());

                    tempmonster.setEvolvesteps(tempmonster.getEvolvesteps() - stepsearned);
                    db.journeyDao().update(tempjourney);
                    db.journeyDao().updateMonster(tempmonster);
                });
                findViewById(R.id.playagain).setVisibility(View.VISIBLE);
                findViewById(R.id.cancel).setVisibility(View.VISIBLE);
                return;
            }

        }

        if(snakedirection != 0){
            for(int i = snakePositions.size()-2; i > 0 ; i--){
                //check if snake head hit itself
                if(snakePositions.get(0).getYposition() == snakePositions.get(i).getYposition() && snakePositions.get(0).getXposition() == snakePositions.get(i).getXposition()){
                    int stepsearned = (snakePositions.size() - 2)*7;
                    countstring = "Steps Earned: " + stepsearned;
                    counttext.setText(countstring);
                    AsyncTask.execute(() -> {
                        AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
                        Journey tempjourney = db.journeyDao().getJourney().get(0);
                        Monster tempmonster = db.journeyDao().getMonster().get(0);
                        tempjourney.addStepstoJourney(stepsearned,tempmonster.getHatched());
                        tempmonster.setEvolvesteps(tempmonster.getEvolvesteps() - stepsearned);
                        db.journeyDao().update(tempjourney);
                        db.journeyDao().updateMonster(tempmonster);
                    });
                    findViewById(R.id.playagain).setVisibility(View.VISIBLE);
                    findViewById(R.id.cancel).setVisibility(View.VISIBLE);
                    return;
                }
                snakePositions.get(i).setYposition(snakePositions.get(i-1).getYposition());
                snakePositions.get(i).setXposition(snakePositions.get(i-1).getXposition());
            }
        }
        snakePositions.get(0).setXposition(xposition);
        snakePositions.get(0).setYposition(yposition);

        ImageView rightscroll = findViewById(R.id.right_arrow);
        ImageView leftscroll = findViewById(R.id.left_arrow);
        rightscroll.clearAnimation();
        leftscroll.clearAnimation();
        rightscroll.setVisibility(View.INVISIBLE);
        leftscroll.setVisibility(View.INVISIBLE);

        Handler h = new Handler();
        h.postDelayed(() -> snakeMove(snakePositions), 200);

    }

    /**
     * add a block to our view for the snake to eat
     * @param snakePositions our arraylist of snake positions
     */
    public void addSnakeSection(ArrayList<SnakePosition> snakePositions){
        ConstraintSet set = new ConstraintSet();
        ConstraintLayout templayout = findViewById(R.id.game_spot);
        ImageView childView = new ImageView(this);
        childView.setBackgroundResource(R.drawable.training_touch);
        // set view id, else getId() returns -1
        int newviewid = View.generateViewId();
        childView.setId(newviewid);
        Random ran = new Random();
        int xposition = ran.nextInt(10);
        int yposition = ran.nextInt(10);

        for(SnakePosition snakePosition : snakePositions){
            if(snakePosition.getXposition() == xposition && snakePosition.getYposition() == yposition){
                xposition = ran.nextInt(10);
                yposition = ran.nextInt(10);
            }
        }

        set.clone(templayout);
        switch(xposition){
            case 0:
                set.connect(childView.getId(),ConstraintSet.START,R.id.vguideline0,ConstraintSet.START,0);
                break;
            case 1:
                set.connect(childView.getId(),ConstraintSet.START,R.id.vguideline1,ConstraintSet.START,0);
                break;
            case 2:
                set.connect(childView.getId(),ConstraintSet.START,R.id.vguideline2,ConstraintSet.START,0);
                break;
            case 3:
                set.connect(childView.getId(),ConstraintSet.START,R.id.vguideline3,ConstraintSet.START,0);
                break;
            case 4:
                set.connect(childView.getId(),ConstraintSet.START,R.id.vguideline4,ConstraintSet.START,0);
                break;
            case 5:
                set.connect(childView.getId(),ConstraintSet.START,R.id.vguideline5,ConstraintSet.START,0);
                break;
            case 6:
                set.connect(childView.getId(),ConstraintSet.START,R.id.vguideline6,ConstraintSet.START,0);
                break;
            case 7:
                set.connect(childView.getId(),ConstraintSet.START,R.id.vguideline7,ConstraintSet.START,0);
                break;
            case 8:
                set.connect(childView.getId(),ConstraintSet.START,R.id.vguideline8,ConstraintSet.START,0);
                break;
            case 9:
                set.connect(childView.getId(),ConstraintSet.START,R.id.vguideline9,ConstraintSet.START,0);
                break;
        }

        switch(yposition){
            case 0:
                set.connect(childView.getId(),ConstraintSet.TOP,R.id.guideline0,ConstraintSet.TOP,0);
                break;
            case 1:
                set.connect(childView.getId(),ConstraintSet.TOP,R.id.guideline1,ConstraintSet.TOP,0);
                break;
            case 2:
                set.connect(childView.getId(),ConstraintSet.TOP,R.id.guideline2,ConstraintSet.TOP,0);
                break;
            case 3:
                set.connect(childView.getId(),ConstraintSet.TOP,R.id.guideline3,ConstraintSet.TOP,0);
                break;
            case 4:
                set.connect(childView.getId(),ConstraintSet.TOP,R.id.guideline4,ConstraintSet.TOP,0);
                break;
            case 5:
                set.connect(childView.getId(),ConstraintSet.TOP,R.id.guideline5,ConstraintSet.TOP,0);
                break;
            case 6:
                set.connect(childView.getId(),ConstraintSet.TOP,R.id.guideline6,ConstraintSet.TOP,0);
                break;
            case 7:
                set.connect(childView.getId(),ConstraintSet.TOP,R.id.guideline7,ConstraintSet.TOP,0);
                break;
            case 8:
                set.connect(childView.getId(),ConstraintSet.TOP,R.id.guideline8,ConstraintSet.TOP,0);
                break;
            case 9:
                set.connect(childView.getId(),ConstraintSet.TOP,R.id.guideline9,ConstraintSet.TOP,0);
                break;
        }
        templayout.addView(childView, 0);
        set.constrainHeight(childView.getId(), 0);
        set.constrainWidth(childView.getId(), 0);
        set.constrainPercentHeight(childView.getId(), 0.1f);
        set.constrainPercentWidth(childView.getId(), 0.1f);
        set.applyTo(templayout);
        snakePositions.add(new SnakePosition(xposition,yposition,newviewid));
    }

    //async task to initialize hunger bar
    static private class HungerFill extends AsyncTask<String,TextView,String>{
        private final WeakReference<Activity> weakActivity;

        public HungerFill(Activity myActivity){
            this.weakActivity = new WeakReference<>(myActivity);
        }

        private int hunger;
        private int currentarrayid;
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(weakActivity.get());
            Monster temp = db.journeyDao().getMonster().get(0);
            hunger = temp.getHunger();
            currentarrayid = temp.getArrayid();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            //where max clip is 10000, hunger is in increments of 8
            ImageView temp = weakActivity.get().findViewById(R.id.hungerfill);
            ClipDrawable hungerfill = (ClipDrawable) temp.getDrawable();
            hungerfill.setLevel(hunger*1250);

            new Minigame().selectedIcon(currentarrayid, weakActivity.get());
        }
    }

    //async task for starting minigames, lowers
    static private class StartMinigame extends AsyncTask<String,TextView,String>{
        private final WeakReference<Activity> weakActivity;

        public StartMinigame(Activity myActivity){
            this.weakActivity = new WeakReference<>(myActivity);
        }

        private int hunger;
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(weakActivity.get());
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
            ImageView temp = weakActivity.get().findViewById(R.id.hungerfill);
            ClipDrawable hungerfill = (ClipDrawable) temp.getDrawable();
            hungerfill.setLevel(hunger*1250);
        }
    }
}
