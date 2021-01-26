package com.application.monsterjourney;

import android.animation.TimeAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StyleableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Ranch extends AppCompatActivity {
    public static final String PREFS_NAME = "MyJourneyFile";
    private ImageView imageView, imageView2, imageView3, imageView4, imageView5;
    private View eggconfirmView, home;
    private List<RanchContainer> ranchContainerList;

    private Button backbutton;
    private TimeAnimator monsterwalk;

    public List<Monster> monsterList;
    private int currentmonster;

    private Monster deletedmonster;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        currentmonster = 0;

        //TODO add a way to release monsters from party, creating an empty space


        final FrameLayout frmlayout = findViewById(R.id.placeholder);
        LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        assert aboutinflater != null;
        home = aboutinflater.inflate(R.layout.ranch_screen, (ViewGroup)null);
        boolean isbought = settings.getBoolean("isbought", false);
        AdView mAdView = findViewById(R.id.adView);
        Activity mainActivity = this;

        frmlayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                frmlayout.addView(home,0);

                imageView = home.findViewById(R.id.monster_icon);
                imageView2 = home.findViewById(R.id.monster_icon2);
                imageView3 = home.findViewById(R.id.monster_icon3);
                imageView4 = home.findViewById(R.id.monster_icon4);
                imageView5 = home.findViewById(R.id.monster_icon5);
                backbutton = findViewById(R.id.back);

                backbutton.setOnClickListener(v -> finish());

                findViewById(R.id.training_popup).setOnClickListener(v ->{
                    //music.release();
                    Intent intent = new Intent(getApplicationContext(), Training.class);
                    startActivity(intent);
                    //where right side is current view
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                });

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

                DisplayMonsters runner = new DisplayMonsters(mainActivity);
                runner.execute();

                frmlayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    //the ontouch listeners are purely for visuals and not usability so warning is suppressed
    @SuppressLint("ClickableViewAccessibility")
    public void showView(){
        TextView monstercount = findViewById(R.id.partycount);
        String partycount = monsterList.size() + "/5";
        findViewById(R.id.layout2).setVisibility(View.GONE);
        findViewById(R.id.layout3).setVisibility(View.GONE);
        findViewById(R.id.layout4).setVisibility(View.GONE);
        findViewById(R.id.layout5).setVisibility(View.GONE);
        imageView.setVisibility(View.INVISIBLE);
        imageView2.setVisibility(View.INVISIBLE);
        imageView3.setVisibility(View.INVISIBLE);
        imageView4.setVisibility(View.INVISIBLE);
        imageView5.setVisibility(View.INVISIBLE);
        monstercount.setText(partycount);
        if(monsterList.size() < 5){
            findViewById(R.id.newegg).setVisibility(View.VISIBLE);
        }
        findViewById(R.id.selector1).setEnabled(false);
        findViewById(R.id.delete1).setEnabled(false);
        int selectedindex = 0;
        @StyleableRes int index = 4;
        ranchContainerList = new ArrayList<>();
        Random ran = new Random();

        for(Monster monster: monsterList){
            int currentarrayid = monster.getArrayid();
            String monstername = monster.getName();
            TypedArray array = getBaseContext().getResources().obtainTypedArray(currentarrayid);
            int resource = array.getResourceId(index,R.drawable.egg_idle);
            switch (selectedindex){
                case 0:
                    imageView.setVisibility(View.VISIBLE);
                    ImageView monsterimage = findViewById(R.id.monster1);
                    monsterimage.setImageDrawable(ContextCompat.getDrawable(this,resource));
                    TextView textView = findViewById(R.id.name1);
                    textView.setText(monstername);
                    imageView.setImageDrawable(ContextCompat.getDrawable(this,resource));
                    AnimationDrawable monsteranimator = (AnimationDrawable) imageView.getDrawable();
                    monsteranimator.start();
                    ranchContainerList.add(new RanchContainer(imageView, imageView.getWidth(), imageView.getHeight()));
                    ranchContainerList.get(0).setWidth(ran.nextFloat()*(home.getWidth()-imageView.getWidth()));
                    ranchContainerList.get(0).setHeight(ran.nextFloat()*(home.getHeight()-imageView.getHeight()));
                    imageView.setOnTouchListener((view, event) -> {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            ranchContainerList.get(0).setSelected(true);
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            ranchContainerList.get(0).setSelected(false);
                        }
                        else if(event.getAction() == MotionEvent.ACTION_MOVE){
                            ranchContainerList.get(0).setWidth(event.getRawX() - view.getWidth());
                            ranchContainerList.get(0).setHeight(event.getRawY() - view.getHeight());
                        }
                        return true;
                    });
                    break;
                case 1:
                    imageView2.setVisibility(View.VISIBLE);
                    findViewById(R.id.layout2).setVisibility(View.VISIBLE);
                    ImageView monsterimage2 = findViewById(R.id.monster2);
                    monsterimage2.setImageDrawable(ContextCompat.getDrawable(this,resource));
                    TextView textView2 = findViewById(R.id.name2);
                    textView2.setText(monstername);
                    imageView2.setImageDrawable(ContextCompat.getDrawable(this,resource));
                    AnimationDrawable monsteranimator2 = (AnimationDrawable) imageView2.getDrawable();
                    monsteranimator2.start();
                    ranchContainerList.add(new RanchContainer(imageView2, imageView2.getWidth(), imageView2.getHeight()));
                    ranchContainerList.get(1).setWidth(ran.nextFloat()*(home.getWidth()-imageView2.getWidth()));
                    ranchContainerList.get(1).setHeight(ran.nextFloat()*(home.getHeight()-imageView2.getHeight()));
                    imageView2.setOnTouchListener((view, event) -> {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            ranchContainerList.get(1).setSelected(true);
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            ranchContainerList.get(1).setSelected(false);
                        }
                        else if(event.getAction() == MotionEvent.ACTION_MOVE){
                            ranchContainerList.get(1).setWidth(event.getRawX() - view.getWidth());
                            ranchContainerList.get(1).setHeight(event.getRawY() - view.getHeight());
                        }
                        return true;
                    });
                    break;
                case 2:
                    imageView3.setVisibility(View.VISIBLE);
                    findViewById(R.id.layout3).setVisibility(View.VISIBLE);
                    ImageView monsterimage3 = findViewById(R.id.monster3);
                    monsterimage3.setImageDrawable(ContextCompat.getDrawable(this,resource));
                    TextView textView3 = findViewById(R.id.name3);
                    textView3.setText(monstername);
                    imageView3.setImageDrawable(ContextCompat.getDrawable(this,resource));
                    AnimationDrawable monsteranimator3 = (AnimationDrawable) imageView3.getDrawable();
                    monsteranimator3.start();
                    ranchContainerList.add(new RanchContainer(imageView3, imageView3.getWidth(), imageView3.getHeight()));
                    ranchContainerList.get(2).setWidth(ran.nextFloat()*(home.getWidth()-imageView3.getWidth()));
                    ranchContainerList.get(2).setHeight(ran.nextFloat()*(home.getHeight()-imageView3.getHeight()));
                    imageView3.setOnTouchListener((view, event) -> {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            ranchContainerList.get(2).setSelected(true);
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            ranchContainerList.get(2).setSelected(false);
                        }
                        else if(event.getAction() == MotionEvent.ACTION_MOVE){
                            ranchContainerList.get(2).setWidth(event.getRawX() - view.getWidth());
                            ranchContainerList.get(2).setHeight(event.getRawY() - view.getHeight());
                        }
                        return true;
                    });
                    break;
                case 3:
                    imageView4.setVisibility(View.VISIBLE);
                    findViewById(R.id.layout4).setVisibility(View.VISIBLE);
                    ImageView monsterimage4 = findViewById(R.id.monster4);
                    monsterimage4.setImageDrawable(ContextCompat.getDrawable(this,resource));
                    TextView textView4 = findViewById(R.id.name4);
                    textView4.setText(monstername);
                    imageView4.setImageDrawable(ContextCompat.getDrawable(this,resource));
                    AnimationDrawable monsteranimator4 = (AnimationDrawable) imageView4.getDrawable();
                    monsteranimator4.start();
                    ranchContainerList.add(new RanchContainer(imageView4, imageView4.getWidth(), imageView4.getHeight()));
                    ranchContainerList.get(3).setWidth(ran.nextFloat()*(home.getWidth()-imageView4.getWidth()));
                    ranchContainerList.get(3).setHeight(ran.nextFloat()*(home.getHeight()-imageView4.getHeight()));
                    imageView4.setOnTouchListener((view, event) -> {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            ranchContainerList.get(3).setSelected(true);
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            ranchContainerList.get(3).setSelected(false);
                        }
                        else if(event.getAction() == MotionEvent.ACTION_MOVE){
                            ranchContainerList.get(3).setWidth(event.getRawX() - view.getWidth());
                            ranchContainerList.get(3).setHeight(event.getRawY() - view.getHeight());
                        }
                        return true;
                    });
                    break;
                case 4:
                    imageView5.setVisibility(View.VISIBLE);
                    findViewById(R.id.layout5).setVisibility(View.VISIBLE);
                    ImageView monsterimage5 = findViewById(R.id.monster5);
                    monsterimage5.setImageDrawable(ContextCompat.getDrawable(this,resource));
                    TextView textView5 = findViewById(R.id.name5);
                    textView5.setText(monstername);
                    imageView5.setImageDrawable(ContextCompat.getDrawable(this,resource));
                    AnimationDrawable monsteranimator5 = (AnimationDrawable) imageView5.getDrawable();
                    monsteranimator5.start();
                    ranchContainerList.add(new RanchContainer(imageView5, imageView5.getWidth(), imageView5.getHeight()));
                    ranchContainerList.get(4).setWidth(ran.nextFloat()*(home.getWidth()-imageView5.getWidth()));
                    ranchContainerList.get(4).setHeight(ran.nextFloat()*(home.getHeight()-imageView5.getHeight()));
                    imageView5.setOnTouchListener((view, event) -> {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            ranchContainerList.get(4).setSelected(true);
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            ranchContainerList.get(4).setSelected(false);
                        }
                        else if(event.getAction() == MotionEvent.ACTION_MOVE){
                            ranchContainerList.get(4).setWidth(event.getRawX() - view.getWidth());
                            ranchContainerList.get(4).setHeight(event.getRawY() - view.getHeight());
                        }
                        return true;
                    });
                    break;
            }
            array.recycle();
            selectedindex++;
        }

        if(monsterwalk == null){
            monsterwalk = new TimeAnimator();
            monsterwalk.setTimeListener((animation, totalTime, deltaTime) -> {
                for(RanchContainer ranchContainer : ranchContainerList) {
                    ranchContainer.compareView(ranchContainerList);
                    if (!ranchContainer.isSelected()) {
                        //turn view the other way if it is past a border
                        if (ranchContainer.getWidth() + ranchContainer.getImageView().getWidth() > home.getWidth()) {
                            ranchContainer.setRightface(false);
                        } else if (ranchContainer.getWidth() < 0) {
                            ranchContainer.setRightface(true);
                        }
                        if (ranchContainer.getHeight() + ranchContainer.getImageView().getHeight() > home.getHeight()) {
                            ranchContainer.setDownface(false);
                        } else if (ranchContainer.getHeight() < 0) {
                            ranchContainer.setDownface(true);
                        }
                        if (ranchContainer.isRightface()) {
                            if (!ranchContainer.isIsoverlapping()){
                                ranchContainer.getImageView().setScaleX(-1);
                            }
                            ranchContainer.setWidth(ranchContainer.getWidth() + home.getWidth() * 0.002f);
                        } else {
                            if (!ranchContainer.isIsoverlapping()){
                                ranchContainer.getImageView().setScaleX(1);
                            }
                            ranchContainer.setWidth(ranchContainer.getWidth() - home.getWidth() * 0.002f);

                        }
                        if (ranchContainer.isDownface()) {
                            ranchContainer.setHeight(ranchContainer.getHeight() + home.getHeight() * 0.002f);
                        } else {
                            ranchContainer.setHeight(ranchContainer.getHeight() - home.getHeight() * 0.002f);
                        }
                    }
                    ranchContainer.getImageView().setTranslationX(ranchContainer.getWidth());
                    ranchContainer.getImageView().setTranslationY(ranchContainer.getHeight());
                }
            });
            monsterwalk.start();
        }

    }


    /**
     * Bring up option to select a new egg to raise
     * @param view the new egg button clicked
     */
    public void newMonster(View view){
        if(eggconfirmView != null){
            return;
        }
        LayoutInflater confirminflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert confirminflater != null;
        eggconfirmView = confirminflater.inflate(R.layout.confirm_popup, null);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow confirmWindow = new PopupWindow(eggconfirmView, width2, height2, true);
        confirmWindow.setOutsideTouchable(false);
        confirmWindow.setOnDismissListener(()->eggconfirmView = null);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            confirmWindow.setElevation(20);
        }
        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        confirmWindow.setAnimationStyle(R.style.PopupAnimation);
        confirmWindow.showAtLocation(findViewById(R.id.placeholder), Gravity.CENTER, 0, 0);
        eggconfirmView.findViewById(R.id.close).setOnClickListener(v -> confirmWindow.dismiss());

        eggconfirmView.findViewById(R.id.back).setOnClickListener(v -> confirmWindow.dismiss());

        eggconfirmView.findViewById(R.id.confirm).setOnClickListener(v ->{
            confirmWindow.dismiss();
            SelectNewEgg runner = new SelectNewEgg(this);
            runner.execute();
        } );

        TextView message = eggconfirmView.findViewById(R.id.confimation_text);
        message.setText(getText(R.string.NewEggConfirm));

    }


    public void selectMonster(View v){
        Monster tempmonster = Monster.populateData();
        findViewById(R.id.selector1).setEnabled(true);
        findViewById(R.id.selector2).setEnabled(true);
        findViewById(R.id.selector3).setEnabled(true);
        findViewById(R.id.selector4).setEnabled(true);
        findViewById(R.id.selector5).setEnabled(true);
        findViewById(R.id.delete1).setEnabled(true);
        findViewById(R.id.delete2).setEnabled(true);
        findViewById(R.id.delete3).setEnabled(true);
        findViewById(R.id.delete4).setEnabled(true);
        findViewById(R.id.delete5).setEnabled(true);
        if(v.getId() == R.id.selector1){
            tempmonster.updateMonster(monsterList.get(currentmonster));
            monsterList.get(currentmonster).updateMonster(monsterList.get(0));
            monsterList.get(0).updateMonster(tempmonster);
            currentmonster = 0;
            findViewById(R.id.delete1).setEnabled(false);
        }
        else if (v.getId() == R.id.selector2){
            tempmonster.updateMonster(monsterList.get(currentmonster));
            monsterList.get(currentmonster).updateMonster(monsterList.get(1));
            monsterList.get(1).updateMonster(tempmonster);
            tempmonster.updateMonster(monsterList.get(currentmonster));
            currentmonster = 1;
            findViewById(R.id.delete2).setEnabled(false);
        }
        else if(v.getId() == R.id.selector3){
            tempmonster.updateMonster(monsterList.get(currentmonster));
            monsterList.get(currentmonster).updateMonster(monsterList.get(2));
            monsterList.get(2).updateMonster(tempmonster);
            currentmonster = 2;
            findViewById(R.id.delete3).setEnabled(false);
        }
        else if (v.getId() == R.id.selector4){
            tempmonster.updateMonster(monsterList.get(currentmonster));
            monsterList.get(currentmonster).updateMonster(monsterList.get(3));
            monsterList.get(3).updateMonster(tempmonster);
            tempmonster.updateMonster(monsterList.get(currentmonster));
            currentmonster = 3;
            findViewById(R.id.delete4).setEnabled(false);
        }
        else if (v.getId() == R.id.selector5){
            tempmonster.updateMonster(monsterList.get(currentmonster));
            monsterList.get(currentmonster).updateMonster(monsterList.get(4));
            monsterList.get(4).updateMonster(tempmonster);
            tempmonster.updateMonster(monsterList.get(currentmonster));
            currentmonster = 4;
            findViewById(R.id.delete5).setEnabled(false);
        }
        v.setEnabled(false);
        AsyncTask.execute(()->{
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            Journey tempjourney = db.journeyDao().getJourney().get(0);
            tempjourney.setMatching(false);
            if(!monsterList.get(0).getHatched()){
                tempjourney.setEventsteps(monsterList.get(0).getEvolvesteps());
                if(monsterList.get(0).getEvolvesteps() > 0){
                    tempjourney.setEventreached(false);
                }

            }
            db.journeyDao().update(tempjourney);
            db.journeyDao().updateMonster(monsterList);

        });

    }

    /**
     * release monster
     * @param view the button which is pressed to delete the monster
     */
    public void deleteMonster(View view){
        if(view.getId() == R.id.delete1){
            deletedmonster = monsterList.get(0);
        }
        else if (view.getId() == R.id.delete2){
            deletedmonster = monsterList.get(1);
        }
        else if(view.getId() == R.id.delete3){
            deletedmonster = monsterList.get(2);
        }
        else if (view.getId() == R.id.delete4){
            deletedmonster = monsterList.get(3);
        }
        else if (view.getId() == R.id.selector5){
            deletedmonster = monsterList.get(4);
        }

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
            DeleteMonster runner = new DeleteMonster(this, deletedmonster);
            runner.execute();
        } );

        TextView message = confirmView.findViewById(R.id.confimation_text);
        message.setText(getText(R.string.ReleaseConfirm));
    }


    /**
     * AsyncTask to start the care
     */
    static private class DisplayMonsters extends AsyncTask<String,TextView,String> {
        private List<Monster> monsterList;
        // Weak references will still allow the Activity to be garbage-collected
        private final WeakReference<Activity> weakActivity;

        public DisplayMonsters(Activity myActivity){
            this.weakActivity = new WeakReference<>(myActivity);
        }
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.getInstance(weakActivity.get());
            monsterList = db.journeyDao().getMonster();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Ranch activity = (Ranch) weakActivity.get();
            activity.monsterList = monsterList;
            activity.showView();

        }
    }

    /**
     * AsyncTask to start the care
     */
    static private class SelectNewEgg extends AsyncTask<String,TextView,String> {
        // Weak references will still allow the Activity to be garbage-collected
        private final WeakReference<Activity> weakActivity;

        public SelectNewEgg(Activity myActivity){
            this.weakActivity = new WeakReference<>(myActivity);
        }
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.getInstance(weakActivity.get());
            Monster currentmonster = db.journeyDao().getMonster().get(0);
            Monster nextMonster = Monster.populateData();
            nextMonster.updateMonster(currentmonster);
            db.journeyDao().insertMonster(nextMonster);
            Journey tempjourney = db.journeyDao().getJourney().get(0);
            tempjourney.setEventtype(0);
            tempjourney.setMatching(false);
            tempjourney.setEventsteps(100);
            currentmonster.newEgg(R.array.enigma_egg);
            db.journeyDao().updateMonster(currentmonster);
            db.journeyDao().update(tempjourney);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Ranch activity = (Ranch) weakActivity.get();
            //music.release();
            Intent intent = new Intent(weakActivity.get(), EggSelect.class);
            activity.startActivity(intent);
            //where right side is current view
            activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            activity.finish();
        }
    }

    /**
     * AsyncTask to start the care
     */
    static private class DeleteMonster extends AsyncTask<String,TextView,String> {
        // Weak references will still allow the Activity to be garbage-collected
        private final WeakReference<Activity> weakActivity;
        Monster tempmonster;

        public DeleteMonster(Activity myActivity, Monster tempmonster){
            this.weakActivity = new WeakReference<>(myActivity);
            this.tempmonster = tempmonster;
        }
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.getInstance(weakActivity.get());
            db.journeyDao().insertHistory(new History(tempmonster.getGeneration(), tempmonster.getArrayid(), tempmonster.getName()));
            db.journeyDao().delete(tempmonster);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            //music.release();
            DisplayMonsters runner = new DisplayMonsters(weakActivity.get());
            runner.execute();
        }
    }



}
