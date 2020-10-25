package com.application.monsterjourney;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Build;
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
import androidx.preference.PreferenceManager;

import java.util.ArrayList;

public class EggSelect extends AppCompatActivity {
    /**
     * our selector for which egg to start the journey with, can be selected again at any time with an adult stage Monster.
     *
     * egganimator: the animation selector for each egg
     */
    public static final String PREFS_NAME = "MyJourneyFile";
    private AnimationDrawable egganimator;
    private ImageView imageView;
    private int selectedegg;
    private int totaleggs;
    private ArrayList<Egg> eggs;
    private int selectedid;
    private AppDatabase db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_egg_select);
        selectedegg = 0;
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        //SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, 0).edit();
        editor.putBoolean(String.valueOf(R.array.basic_egg), true);
        editor.putBoolean(String.valueOf(R.array.dino_egg),true);
        editor.apply();



        //totaleggs = settings.getInt("total eggs",2);
        TypedArray egglist = getResources().obtainTypedArray(R.array.egg_list);
        totaleggs = egglist.length();

        eggs = new ArrayList<>();
        for(int i = 0; i < totaleggs; i++){
            eggs.add(new Egg(egglist.getResourceId(i,R.array.basic_egg),settings.getBoolean(String.valueOf(egglist.getResourceId(i,R.array.basic_egg)),false)));
        }
        egglist.recycle();

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

                findViewById(R.id.right_arrow).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //if(!isplaying)soundPool.load(context, R.raw.pressdown, 1);
                        selectedegg = (selectedegg+1)%totaleggs;
/*                        while(!eggs.get(selectedegg).getEggUnlocked()){
                            selectedegg = (selectedegg+1)%totaleggs;
                        }*/
                        switchviews(selectedegg, imageView, title, description);
                    }
                });
                findViewById(R.id.left_arrow).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //if(!isplaying)soundPool.load(context, R.raw.pressdown, 1);
                        selectedegg = (selectedegg+totaleggs-1)%totaleggs;
/*                        while(!eggs.get(selectedegg).getEggUnlocked()){
                            selectedegg = (selectedegg + totaleggs -1)%totaleggs;
                        }*/
                        switchviews(selectedegg, imageView, title, description);
                    }
                });

                findViewById(R.id.confirm).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmSelection();
                    }
                });

                switchviews(selectedegg, imageView, title, description);
                frmlayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

    }

    private void switchviews(int viewselected, ImageView imageView, TextView eggtitle, TextView description){
/*        int backgroundAnimation = R.drawable.egg_idle;
        int title = R.string.basic_egg_title;
        int eggtext = R.string.basic_egg_description;*/
        TypedArray array = getResources().obtainTypedArray(eggs.get(viewselected).getEggnumber());
        @StyleableRes int id = 4;
        int backgroundAnimation = array.getResourceId(id, R.drawable.egg_idle);
        int title = array.getResourceId(id-2,R.drawable.egg_idle);
        int eggtext = array.getResourceId(id-1,R.drawable.egg_idle);
        selectedid = eggs.get(viewselected).getEggnumber();
/*        switch(viewselected){
            case 0:
                selectedid = R.array.basic_egg;
                backgroundAnimation = R.drawable.egg_idle;
                title = R.string.basic_egg_title;
                eggtext = R.string.basic_egg_description;
                break;
            case 1:
                selectedid = R.array.dino_egg;
                backgroundAnimation = R.drawable.egg_dino_idle;
                title = R.string.dino_egg_title;
                eggtext = R.string.dino_egg_description;
                break;
        }*/
        array.recycle();
        imageView.setBackgroundResource(backgroundAnimation);
        egganimator = (AnimationDrawable) imageView.getBackground();
        egganimator.start();
        eggtitle.setText(getText(title));
        description.setText(getText(eggtext));
    }

    public void confirmSelection(){

        AsyncTask.execute(new Runnable()
        {
            @Override
            public void run()
            {
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
            }
        });

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
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
}
