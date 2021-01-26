package com.application.monsterjourney;

import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.TableLayout;
import android.widget.TextView;
import androidx.annotation.StyleableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import java.util.List;

public class Library extends AppCompatActivity {
    public static final String PREFS_NAME = "MyJourneyFile";
    private ImageView imageView;
    private AnimationDrawable spriteanimator;

    private NumberPicker picker1;
    private String[] pickervals;

    private Button backbutton;
    private TextView description;
    private TextView title, libraryindex;

    private @StyleableRes int selected;
    private int selectedarray;

    private List<UnlockedMonster> unlockedMonsterList;

    private View librarypopup;
    private MediaPlayer music;
    private boolean isplaying;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        selected = 0;

        AsyncTask.execute(() -> {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            unlockedMonsterList = db.journeyDao().getUnlockedMonster();
        });

        final FrameLayout frmlayout = findViewById(R.id.placeholder);
        LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        assert aboutinflater != null;
        final View home = aboutinflater.inflate(R.layout.home_screen, (ViewGroup)null);
        boolean isbought = settings.getBoolean("isbought", false);
        AdView mAdView = findViewById(R.id.adView);

        frmlayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                frmlayout.addView(home,0);

                imageView = home.findViewById(R.id.monster_icon);
                title = findViewById(R.id.Title);
                description = findViewById(R.id.content);
                backbutton = findViewById(R.id.back);
                libraryindex = findViewById(R.id.libraryindex);

                picker1 = findViewById(R.id.picker);
                imageView.setBackgroundResource(R.drawable.egg_idle);
                spriteanimator = (AnimationDrawable) imageView.getBackground();

                picker1.setMaxValue(3);
                picker1.setMinValue(0);

                ImageView rightscroll = findViewById(R.id.right_arrow);
                ImageView leftscroll = findViewById(R.id.left_arrow);
                Animation leftscrollanimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.leftarrow);

                Animation rightscrollanimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rightarrow);
                leftscroll.startAnimation(rightscrollanimation);
                rightscroll.startAnimation(leftscrollanimation);

                pickervals = new String[] {"Egg", "Baby", "Child","Adult"};

                picker1.setDisplayedValues(pickervals);

                picker1.setOnValueChangedListener((numberPicker, i, i1) -> {
                    int valuePicker1 = picker1.getValue();
                    setView(valuePicker1);
                });

                backbutton.setOnClickListener(v -> finish());

                findViewById(R.id.right_arrow).setOnClickListener(v -> {
                    //if(!isplaying)soundPool.load(context, R.raw.pressdown, 1);
                    TypedArray temp = getResources().obtainTypedArray(selectedarray);
                    selected = (selected+1)%(temp.length());
                    temp.recycle();

                    showView();
                });
                findViewById(R.id.left_arrow).setOnClickListener(v -> {
                    //if(!isplaying)soundPool.load(context, R.raw.pressdown, 1);
                    TypedArray temp = getResources().obtainTypedArray(selectedarray);
                    selected = (selected+temp.length()-1)%temp.length();
                    temp.recycle();
                    showView();
                });

                findViewById(R.id.library_info_popup).setOnClickListener(v -> showLibraryPopup());

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

                setView(0);

                frmlayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

    }

    @Override
    protected void onResume() {
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        int tempvolume = 80;
        music = MediaPlayer.create(Library.this,R.raw.library);
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

    /**
     * Changes the stage we want to look at
     * @param selection the stage to show
     */
    public void setView(int selection){
        this.selected = 0;
        switch(selection){
            case 0:
                selectedarray = R.array.egg_list;
                break;
            case 1:
                selectedarray = R.array.baby_list;
                break;
            case 2:
                selectedarray = R.array.child_list;
                break;
            case 3:
                selectedarray = R.array.adult_list;
                break;
        }
        unlockedChecker();
        showView();
    }

    /**
     * checks which monster is unlocked first so moving the number selector looks better
     */
    private void unlockedChecker(){
        TypedArray viewarray = getResources().obtainTypedArray(selectedarray);
        for(int i = 0; i < viewarray.length(); i++){
            for(UnlockedMonster unlockedMonster : unlockedMonsterList){
                if(unlockedMonster.getMonsterarrayid() == viewarray.getResourceId(i, R.array.missing_content)){
                    if(unlockedMonster.isDiscovered()){
                        selected = i;
                        viewarray.recycle();
                        return;
                    }
                }
            }
        }
        viewarray.recycle();
    }

    /**
     * shows the current monster and description
     */
    public void showView(){
        if(unlockedMonsterList == null){
            selected = 0;
            return;
        }
        TypedArray viewarray = getResources().obtainTypedArray(selectedarray);
        TypedArray array = getResources().obtainTypedArray(viewarray.getResourceId(selected, R.array.missing_content));

        String selectedview = (selected+1) + "/" + viewarray.length();
        libraryindex.setText(selectedview);

        boolean animate = false;
        int backgroundAnimation = R.drawable.missing_content;
        int monstertitle = R.string.missing_title;
        int monstertext = R.string.missing_description;
        //normally defvalue is false but we loaded it as true for demo
        //settings.getBoolean(String.valueOf(viewarray.getResourceId(selected, R.array.missing_content)),true)
        for(UnlockedMonster unlockedMonster : unlockedMonsterList){
            if(unlockedMonster.getMonsterarrayid() == viewarray.getResourceId(selected, R.array.missing_content)){
                if(unlockedMonster.isDiscovered()){
                    @StyleableRes int tempid = 4;
                    backgroundAnimation = array.getResourceId(tempid,R.drawable.egg_idle);
                    monstertitle = array.getResourceId(tempid-2,R.drawable.egg_idle);
                    animate = true;
                }
                if(unlockedMonster.isUnlocked()){
                    @StyleableRes int tempid = 4;
                    monstertext = array.getResourceId(tempid-1,R.drawable.egg_idle);
                }
            }
        }

        imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(),backgroundAnimation));
        imageView.setBackgroundResource(R.drawable.ic_background_library);
        if(animate){
            spriteanimator = (AnimationDrawable) imageView.getDrawable();
            spriteanimator.start();
        }
        this.title.setText(getText(monstertitle));
        description.setText(getText(monstertext));
        array.recycle();
        viewarray.recycle();
    }

    /**
     * shows the library popup, which can show specific monster info when tapped
     */
    private void showLibraryPopup(){
        if(librarypopup != null){
            return;
        }
        LayoutInflater storeinflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert storeinflater != null;
        librarypopup = storeinflater.inflate(R.layout.library_collection, null);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow libraryWindow = new PopupWindow(librarypopup, width2, height2, true);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            libraryWindow.setElevation(20);
        }
        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        libraryWindow.setAnimationStyle(R.style.PopupAnimation);
        libraryWindow.showAtLocation(findViewById(R.id.placeholder), Gravity.CENTER, 0, 0);
        librarypopup.findViewById(R.id.close).setOnClickListener(v -> libraryWindow.dismiss());

        libraryWindow.setOnDismissListener(() -> librarypopup = null);

        TypedArray viewarray = getResources().obtainTypedArray(R.array.egg_list);
        TypedArray viewarray2 = getResources().obtainTypedArray(R.array.baby_list);
        TypedArray viewarray3 = getResources().obtainTypedArray(R.array.child_list);
        TypedArray viewarray4 = getResources().obtainTypedArray(R.array.adult_list);

        TableLayout egg_layout = librarypopup.findViewById(R.id.egg_table);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setWeightSum(5);

        LinearLayout.LayoutParams linearparams = new LinearLayout.LayoutParams(0, 200, 1f);
        for(int i = 0; i < viewarray.length(); i++){
            TypedArray array = getResources().obtainTypedArray(viewarray.getResourceId(i, R.array.missing_content));
            int backgroundAnimation = R.drawable.missing_content;
            ImageView newView = new ImageView(this);
            for(UnlockedMonster unlockedMonster : unlockedMonsterList){
                if(unlockedMonster.getMonsterarrayid() == viewarray.getResourceId(i, R.array.missing_content)){
                    if(unlockedMonster.isDiscovered()){
                        @StyleableRes int tempid = 4;
                        backgroundAnimation = array.getResourceId(tempid,R.drawable.egg_idle);
                        int finalI = i;
                        newView.setOnClickListener(v -> {
                            picker1.setValue(0);
                            selectedarray = R.array.egg_list;
                            selected = finalI;
                            libraryWindow.dismiss();
                            showView();
                        });
                    }
                    break;
                }
            }
            newView.setBackgroundResource(R.color.colorAccent);
            newView.setImageDrawable(ContextCompat.getDrawable(this,backgroundAnimation));
            newView.setLayoutParams(linearparams);
            newView.setScaleType(ImageView.ScaleType.FIT_XY);
            newView.setPadding(2,2,2,2);

            linearLayout.addView(newView);

            if((i+1) % 5 == 0) {
                egg_layout.addView(linearLayout);
                linearLayout = new LinearLayout(this);
                linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                linearLayout.setWeightSum(5);

            }
            array.recycle();
        }
        egg_layout.addView(linearLayout);

        linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setWeightSum(5);

        TableLayout baby_layout = librarypopup.findViewById(R.id.baby_table);

        for(int i = 0; i < viewarray2.length(); i++){
            TypedArray array = getResources().obtainTypedArray(viewarray2.getResourceId(i, R.array.missing_content));
            int backgroundAnimation = R.drawable.missing_content;
            ImageView newView = new ImageView(this);
            for(UnlockedMonster unlockedMonster : unlockedMonsterList){
                if(unlockedMonster.getMonsterarrayid() == viewarray2.getResourceId(i, R.array.missing_content)){
                    if(unlockedMonster.isDiscovered()){
                        @StyleableRes int tempid = 4;
                        backgroundAnimation = array.getResourceId(tempid,R.drawable.egg_idle);
                        int finalI = i;
                        newView.setOnClickListener(v -> {
                            picker1.setValue(1);
                            selectedarray = R.array.baby_list;
                            selected = finalI;
                            libraryWindow.dismiss();
                            showView();
                        });
                    }
                    break;
                }
            }

            newView.setBackgroundResource(R.color.colorAccent);
            newView.setImageDrawable(ContextCompat.getDrawable(this,backgroundAnimation));
            newView.setLayoutParams(linearparams);
            newView.setScaleType(ImageView.ScaleType.FIT_XY);
            newView.setPadding(2,2,2,2);

            linearLayout.addView(newView);

            if((i+1) % 5 == 0) {
                baby_layout.addView(linearLayout);
                linearLayout = new LinearLayout(this);
                linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                linearLayout.setWeightSum(5);

            }
            array.recycle();
        }
        baby_layout.addView(linearLayout);

        linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setWeightSum(5);

        TableLayout child_layout = librarypopup.findViewById(R.id.child_table);

        for(int i = 0; i < viewarray3.length(); i++){
            TypedArray array = getResources().obtainTypedArray(viewarray3.getResourceId(i, R.array.missing_content));
            int backgroundAnimation = R.drawable.missing_content;
            ImageView newView = new ImageView(this);
            for(UnlockedMonster unlockedMonster : unlockedMonsterList){
                if(unlockedMonster.getMonsterarrayid() == viewarray3.getResourceId(i, R.array.missing_content)){
                    if(unlockedMonster.isDiscovered()){
                        @StyleableRes int tempid = 4;
                        backgroundAnimation = array.getResourceId(tempid,R.drawable.egg_idle);
                        int finalI = i;
                        newView.setOnClickListener(v -> {
                            picker1.setValue(2);
                            selectedarray = R.array.child_list;
                            selected = finalI;
                            libraryWindow.dismiss();
                            showView();
                        });
                    }
                    break;
                }
            }

            //TODO replace with proper drawable
            newView.setBackgroundResource(R.color.colorAccent);
            newView.setImageDrawable(ContextCompat.getDrawable(this,backgroundAnimation));
            newView.setLayoutParams(linearparams);
            newView.setScaleType(ImageView.ScaleType.FIT_XY);
            newView.setPadding(2,2,2,2);

            linearLayout.addView(newView);
            //TODO displaying children makes the display smaller for some reason
            if((i+1) % 5 == 0) {
                child_layout.addView(linearLayout);
                linearLayout = new LinearLayout(this);
                linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                linearLayout.setWeightSum(5);

            }
            array.recycle();
        }
        child_layout.addView(linearLayout);

        TableLayout adult_layout = librarypopup.findViewById(R.id.adult_table);

        linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setWeightSum(5);

        for(int i = 0; i < viewarray4.length(); i++){
            TypedArray array = getResources().obtainTypedArray(viewarray4.getResourceId(i, R.array.missing_content));
            int backgroundAnimation = R.drawable.missing_content;
            ImageView newView = new ImageView(this);
            for(UnlockedMonster unlockedMonster : unlockedMonsterList){
                if(unlockedMonster.getMonsterarrayid() == viewarray4.getResourceId(i, R.array.missing_content)){
                    if(unlockedMonster.isDiscovered()){
                        @StyleableRes int tempid = 4;
                        backgroundAnimation = array.getResourceId(tempid,R.drawable.egg_idle);
                        int finalI = i;
                        newView.setOnClickListener(v -> {
                            picker1.setValue(3);
                            selectedarray = R.array.adult_list;
                            selected = finalI;
                            libraryWindow.dismiss();
                            showView();
                        });
                    }
                    break;
                }
            }
            newView.setBackgroundResource(R.color.colorAccent);
            newView.setImageDrawable(ContextCompat.getDrawable(this,backgroundAnimation));
            newView.setLayoutParams(linearparams);
            newView.setScaleType(ImageView.ScaleType.FIT_XY);
            newView.setPadding(2,2,2,2);

            linearLayout.addView(newView);

            if((i+1) % 5 == 0) {
                adult_layout.addView(linearLayout);
                linearLayout = new LinearLayout(this);
                linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                linearLayout.setWeightSum(5);

            }

            array.recycle();
        }
        adult_layout.addView(linearLayout);

        viewarray.recycle();
        viewarray2.recycle();
        viewarray3.recycle();
        viewarray4.recycle();


    }

}
