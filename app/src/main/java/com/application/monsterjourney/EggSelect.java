package com.application.monsterjourney;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.StyleableRes;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class EggSelect extends AppCompatActivity {
    private AnimationDrawable egganimator;
    private ImageView imageView;
    private int selectedegg;
    private int totaleggs;
    private ArrayList<Egg> eggs;
    private int selectedid;
    private AppDatabase db;

    private List<UnlockedMonster> unlockedMonsters;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_egg_select);
        selectedegg = 0;
        //set the id for selected id first so it is not null
        selectedid = R.array.enigma_egg;

        //TODO left and right arrows giving divide by 0 error


        CheckUnlocked runner = new CheckUnlocked(this);
        runner.execute();


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

                imageView = home.findViewById(R.id.monster_icon);
                final TextView title = findViewById(R.id.Title);
                final TextView description = findViewById(R.id.content);
                imageView.setBackgroundResource(R.drawable.egg_idle);
                egganimator = (AnimationDrawable) imageView.getBackground();

                ImageView rightscroll = findViewById(R.id.right_arrow);
                ImageView leftscroll = findViewById(R.id.left_arrow);
                Animation leftscrollanimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.leftarrow);

                Animation rightscrollanimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rightarrow);
                leftscroll.startAnimation(rightscrollanimation);
                rightscroll.startAnimation(leftscrollanimation);

                findViewById(R.id.right_arrow).setOnClickListener(v -> {
                    //if(!isplaying)soundPool.load(context, R.raw.pressdown, 1);
                    selectedegg = (selectedegg+1)%totaleggs;
/*                        while(!eggs.get(selectedegg).getEggUnlocked()){
                        selectedegg = (selectedegg+1)%totaleggs;
                    }*/
                    switchviews(selectedegg, imageView, title, description);
                });
                findViewById(R.id.left_arrow).setOnClickListener(v -> {
                    //if(!isplaying)soundPool.load(context, R.raw.pressdown, 1);
                    selectedegg = (selectedegg+totaleggs-1)%totaleggs;
/*                        while(!eggs.get(selectedegg).getEggUnlocked()){
                        selectedegg = (selectedegg + totaleggs -1)%totaleggs;
                    }*/
                    switchviews(selectedegg, imageView, title, description);
                });

                findViewById(R.id.confirm).setOnClickListener(v -> confirmSelection());

                //switchviews(selectedegg, imageView, title, description);
                frmlayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

    }


    /**
     * initialize the egg select view after we
     */
    private void initializeSelect(){
        eggs = new ArrayList<>();
        for(UnlockedMonster unlockedMonster : unlockedMonsters){
            if(unlockedMonster.getStage() == 0 && unlockedMonster.isUnlocked()){
                eggs.add(new Egg(unlockedMonster.getMonsterarrayid(), unlockedMonster.isUnlocked()));
            }
        }
        totaleggs = eggs.size();
    }

    private void switchviews(int viewselected, ImageView imageView, TextView eggtitle, TextView description){
        if(eggs == null){
            return;
        }
/*        int backgroundAnimation = R.drawable.egg_idle;
        int title = R.string.basic_egg_title;
        int eggtext = R.string.basic_egg_description;*/
        TypedArray array = getResources().obtainTypedArray(eggs.get(viewselected).getEggnumber());
        @StyleableRes int id = 4;
        int backgroundAnimation = array.getResourceId(id, R.drawable.egg_idle);
        int title = array.getResourceId(id-2,R.drawable.egg_idle);
        int eggtext = array.getResourceId(id-1,R.drawable.egg_idle);
        selectedid = eggs.get(viewselected).getEggnumber();

        array.recycle();
        imageView.setBackgroundResource(backgroundAnimation);
        egganimator = (AnimationDrawable) imageView.getBackground();
        egganimator.start();
        eggtitle.setText(getText(title));
        description.setText(getText(eggtext));
    }

    public void confirmSelection(){
        if(eggs == null){
            return;
        }

        AsyncTask.execute(() -> {
            db = AppDatabase.buildDatabase(getApplicationContext());
            // run your queries here!
            Journey temp = db.journeyDao().getJourney().get(0);
            temp.setEventsteps(100);
            temp.setEventtype(0);
            temp.setFirsttime(false);
            temp.setEventreached(false);
            db.journeyDao().update(temp);

            Monster tempmonster = db.journeyDao().getMonster().get(0);

            //tempmonster.setArrayid(selectedid);
            tempmonster.newEgg(selectedid);

            db.journeyDao().updateMonster(tempmonster);
        });

//        Intent intent = new Intent(this, MainActivity.class);
//        startActivity(intent);
        //where right side is current view
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        egganimator.start();
        super.onWindowFocusChanged(hasFocus);
    }

    /**
     * override the back button so player can't exit this activity with it
     */
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }

    //perform check for evolving our monster and change view if appropriate
    private static class CheckUnlocked extends AsyncTask<String,String, String>{
        // Weak references will still allow the Activity to be garbage-collected
        private final WeakReference<Activity> weakActivity;
        List<UnlockedMonster> unlockedMonsters;

        public CheckUnlocked(Activity activity){
            weakActivity = new WeakReference<>(activity);
        }
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(weakActivity.get());
            unlockedMonsters = db.journeyDao().getUnlockedMonster();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            EggSelect thisActivity = (EggSelect) weakActivity.get();
            thisActivity.unlockedMonsters = unlockedMonsters;
            thisActivity.initializeSelect();
        }
    }
}
