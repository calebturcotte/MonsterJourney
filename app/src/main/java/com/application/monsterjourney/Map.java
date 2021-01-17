package com.application.monsterjourney;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.StyleableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import java.lang.ref.WeakReference;
import java.util.List;

public class Map extends AppCompatActivity {
    public static final String PREFS_NAME = "MyJourneyFile";
    private ImageView imageView, completedView;
    private Button confirmbutton, backbutton;
    private TextView title, stepsneeded, description;
    private @StyleableRes
    int selected, currentselectedstory;

    private List<UnlockedMonster> unlockedMonsterList;
    private List<CompletedMaps> completedMapsList;
    private int storytype;
    private long storysteps;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);


        final FrameLayout frmlayout = findViewById(R.id.placeholder);
        LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        assert aboutinflater != null;
        final View home = aboutinflater.inflate(R.layout.home_screen, null);
        boolean isbought = settings.getBoolean("isbought", false);
        AdView mAdView = findViewById(R.id.adView);
        Activity mainActivity = this;

        frmlayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                frmlayout.addView(home,0);

                imageView = home.findViewById(R.id.monster_icon);
                completedView = findViewById(R.id.unlockedindicator);
                title = findViewById(R.id.Title);
                stepsneeded = findViewById(R.id.Steps);
                description = findViewById(R.id.content);
                backbutton = findViewById(R.id.back);
                confirmbutton = findViewById(R.id.Confirm);

                ImageView rightscroll = findViewById(R.id.right_arrow);
                ImageView leftscroll = findViewById(R.id.left_arrow);
                Animation leftscrollanimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.leftarrow);

                Animation rightscrollanimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rightarrow);
                leftscroll.startAnimation(rightscrollanimation);
                rightscroll.startAnimation(leftscrollanimation);

                backbutton.setOnClickListener(v -> finish());

                findViewById(R.id.right_arrow).setOnClickListener(v -> {
                    //if(!isplaying)soundPool.load(context, R.raw.pressdown, 1);
                    TypedArray temp = getResources().obtainTypedArray(R.array.egg_list);
                    selected = (selected+1)%(temp.length());
                    temp.recycle();
                    showView();
                });
                findViewById(R.id.left_arrow).setOnClickListener(v -> {
                    //if(!isplaying)soundPool.load(context, R.raw.pressdown, 1);
                    TypedArray temp = getResources().obtainTypedArray(R.array.egg_list);
                    selected = (selected+temp.length()-1)%temp.length();
                    temp.recycle();
                    showView();
                });

                confirmbutton.setOnClickListener(v -> {
                    TypedArray viewarray = getResources().obtainTypedArray(R.array.map_list);
                    storytype = viewarray.getResourceId(selected, R.array.enigma_map);
                    currentselectedstory = selected;

                    ChangeStory runner = new ChangeStory(mainActivity, storytype, completedMapsList);
                    runner.execute();
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

                DisplayMap runner = new DisplayMap(mainActivity);
                runner.execute();
                frmlayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    /**
     * shows the current map and description, as well as steps before boss
     */
    public void showView(){
        //TODO update map_list array descriptions about the maps, and title of the map. Maybe add info on enemies found on the map too
        if(unlockedMonsterList == null){
            //selected = 0;
            return;
        }
        TypedArray viewarray = getResources().obtainTypedArray(R.array.map_list);
        TypedArray viewarray2 = getResources().obtainTypedArray(R.array.egg_list);
        TypedArray array = getResources().obtainTypedArray(viewarray.getResourceId(selected, R.array.missing_content));

        int monstertitle = R.string.missing_title;
        int monstertext = R.string.missing_description;
        confirmbutton.setEnabled(false);
        //normally defvalue is false but we loaded it as true for demo
        ColorMatrix matrix = new ColorMatrix();
        for(UnlockedMonster unlockedMonster : unlockedMonsterList){
            if(unlockedMonster.getMonsterarrayid() == viewarray2.getResourceId(selected, R.array.missing_content)){
                if(unlockedMonster.isUnlocked()){
                    @StyleableRes int tempid = 5;
                    monstertitle = array.getResourceId(tempid,R.drawable.egg_idle);
                    confirmbutton.setEnabled(true);
                    imageView.setImageResource(0);
                    matrix.setSaturation(1);
                    imageView.setColorFilter(new ColorMatrixColorFilter(matrix));
                    stepsneeded.setVisibility(View.VISIBLE);
                    completedView.setVisibility(View.VISIBLE);
                }
                else {
                    imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(),R.drawable.missing_content));
                    matrix.setSaturation(0);
                    imageView.setColorFilter(new ColorMatrixColorFilter(matrix));
                    stepsneeded.setVisibility(View.INVISIBLE);
                    completedView.setVisibility(View.INVISIBLE);
                }
                if(unlockedMonster.isDiscovered()){
                    @StyleableRes int tempid = 6;
                    monstertext = array.getResourceId(tempid,R.drawable.egg_idle);
                }
                break;
            }
        }
        if(selected == currentselectedstory) {
            confirmbutton.setEnabled(false);
            stepsneeded.setText(String.valueOf(storysteps));
            if(completedMapsList.get(selected).isStorycompleted()){
                completedView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_selection_on));
            }
            else {
                completedView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_selection_off));
            }
        }
        else{
            if(completedMapsList.get(selected).isStorycompleted()){
                stepsneeded.setText(getText(R.string.mapcompleted));
                completedView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_selection_on));
            }
            else {
                stepsneeded.setText(getText(R.string.mapnotcompleted));
                completedView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_selection_off));
            }

        }

        @StyleableRes int tempid = 4;
        imageView.setBackgroundResource(array.getResourceId(tempid,R.drawable.mapbackground2));

        this.title.setText(getText(monstertitle));
        description.setText(getText(monstertext));
        array.recycle();
        viewarray.recycle();
        viewarray2.recycle();
    }


    /**
     * AsyncTask to start the care
     */
    static private class DisplayMap extends AsyncTask<String,TextView,String> {
        private List<UnlockedMonster> unlockedMonsterList;
        private List<CompletedMaps> completedMapsList;
        private int storytype;
        private long storysteps;
        // Weak references will still allow the Activity to be garbage-collected
        private final WeakReference<Activity> weakActivity;

        public DisplayMap(Activity myActivity){
            this.weakActivity = new WeakReference<>(myActivity);
        }
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(weakActivity.get());
            unlockedMonsterList = db.journeyDao().getUnlockedMonster();
            storytype = db.journeyDao().getJourney().get(0).getStorytype();
            storysteps = db.journeyDao().getJourney().get(0).getStorysteps();
            completedMapsList = db.journeyDao().getCompletedMaps();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Map activity = (Map) weakActivity.get();
            activity.unlockedMonsterList = unlockedMonsterList;
            activity.completedMapsList = completedMapsList;
            activity.storytype = storytype;
            activity.storysteps = storysteps;
            TypedArray viewarray = weakActivity.get().getResources().obtainTypedArray(R.array.map_list);
            for(int i = 0; i < viewarray.length(); i++){
                if(storytype == viewarray.getResourceId(i, R.array.missing_content)){
                    activity.selected = i;
                    activity.currentselectedstory = i;
                    break;
                }
            }
            activity.showView();
            viewarray.recycle();
        }
    }

    /**
     * AsyncTask to start the care
     */
    static private class ChangeStory extends AsyncTask<String,TextView,String> {
        private List<CompletedMaps> completedMapsList;
        private int storytype;
        private long storysteps;
        // Weak references will still allow the Activity to be garbage-collected
        private final WeakReference<Activity> weakActivity;

        public ChangeStory(Activity myActivity, int storytype, List<CompletedMaps> completedMapsList){
            this.weakActivity = new WeakReference<>(myActivity);
            this.storytype = storytype;
            this.completedMapsList = completedMapsList;
        }
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.buildDatabase(weakActivity.get());
            Journey tempjourney = db.journeyDao().getJourney().get(0);
            tempjourney.setStorytype(storytype);
            //set the story steps to the new steps for the map
            for(CompletedMaps completedMaps : completedMapsList){
                if(completedMaps.getMaparray() == tempjourney.getStorytype()){
                    storysteps = completedMaps.getStorysteps();
                    tempjourney.setStorysteps(storysteps);
                    break;
                }
            }
            db.journeyDao().update(tempjourney);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Map activity = (Map) weakActivity.get();
            activity.storysteps = storysteps;
            activity.showView();
        }
    }


}
