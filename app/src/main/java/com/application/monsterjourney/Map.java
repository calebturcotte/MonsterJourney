package com.application.monsterjourney;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.StyleableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
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
    private MediaPlayer music;
    private boolean isplaying;
    private View mapChangeView;

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
        final View home = aboutinflater.inflate(R.layout.home_screen,findViewById(R.id.parent),false);
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

                confirmbutton.setOnClickListener(v -> mapChange());

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

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        int tempvolume = 80;
        music = MediaPlayer.create(Map.this,R.raw.map);
        music.setLooping(true);
        int currentvolume = settings.getInt("bgvolume", tempvolume);

        music.setVolume((float) currentvolume /100, (float) currentvolume /100);
        isplaying = settings.getBoolean("isplaying",isplaying);
        //music.prepareAsync();

        if(!isplaying)music.start();
    }

    @Override
    protected void onPause() {
        music.release();
        super.onPause();
    }

    /**
     * Bring up option to select a new egg to raise
     */
    public void mapChange(){
        if(mapChangeView != null){
            return;
        }
        LayoutInflater confirminflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert confirminflater != null;
        mapChangeView = confirminflater.inflate(R.layout.confirm_popup,findViewById(R.id.parent), false);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow confirmWindow = new PopupWindow(mapChangeView, width2, height2, true);
        confirmWindow.setOutsideTouchable(false);
        confirmWindow.setOnDismissListener(()->mapChangeView = null);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            confirmWindow.setElevation(20);
        }
        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        confirmWindow.setAnimationStyle(R.style.PopupAnimation);
        confirmWindow.showAtLocation(findViewById(R.id.placeholder), Gravity.CENTER, 0, 0);
        mapChangeView.findViewById(R.id.close).setOnClickListener(v -> confirmWindow.dismiss());

        mapChangeView.findViewById(R.id.back).setOnClickListener(v -> confirmWindow.dismiss());

        mapChangeView.findViewById(R.id.confirm).setOnClickListener(v ->{
            TypedArray viewarray = getResources().obtainTypedArray(R.array.map_list);
            storytype = viewarray.getResourceId(selected, R.array.enigma_map);
            currentselectedstory = selected;
            viewarray.recycle();

            ChangeStory runner = new ChangeStory(this, storytype, completedMapsList);
            runner.execute();
            confirmWindow.dismiss();
        } );

        TextView message = mapChangeView.findViewById(R.id.confimation_text);
        message.setText(getText(R.string.mapchangeconfirm));

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
        if(completedMapsList.get(selected).isIsunlocked()){
            @StyleableRes int tempid = 5;
            monstertitle = array.getResourceId(tempid,R.drawable.egg_idle);
            confirmbutton.setEnabled(true);
            imageView.setImageResource(0);
            matrix.setSaturation(1);
            stepsneeded.setVisibility(View.VISIBLE);
            completedView.setVisibility(View.VISIBLE);
            monstertext = array.getResourceId(tempid+1,R.drawable.egg_idle);
        }else {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(),R.drawable.missing_content));
            matrix.setSaturation(0.3f);
            stepsneeded.setVisibility(View.INVISIBLE);
            completedView.setVisibility(View.INVISIBLE);
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
        imageView.getBackground().setColorFilter(new ColorMatrixColorFilter(matrix));

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
