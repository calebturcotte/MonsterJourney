package com.application.monsterjourney;

import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.StyleableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.List;

public class Library extends AppCompatActivity {
    public static final String PREFS_NAME = "MyJourneyFile";
    private ImageView imageView;
    private AnimationDrawable spriteanimator;

    private NumberPicker picker1;
    private String[] pickervals;

    private Button backbutton;
    private TextView description;
    private TextView title;

    private @StyleableRes int selected;
    private int selectedarray;
    private SharedPreferences settings;

    private List<UnlockedMonster> unlockedMonsterList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);
        settings = getSharedPreferences(PREFS_NAME, 0);
        selected = 0;

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
                unlockedMonsterList = db.journeyDao().getUnlockedMonster();
            }
        });

        final FrameLayout frmlayout = (FrameLayout) findViewById(R.id.placeholder);
        LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final View home = aboutinflater.inflate(R.layout.home_screen, (ViewGroup)null);

        frmlayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                frmlayout.addView(home,0);

                imageView = home.findViewById(R.id.monster_icon);
                title = findViewById(R.id.Title);
                description = findViewById(R.id.content);
                backbutton = findViewById(R.id.back);

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

                picker1.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                        int valuePicker1 = picker1.getValue();
                        setView(valuePicker1);
                    }
                });

                backbutton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });

                findViewById(R.id.right_arrow).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //if(!isplaying)soundPool.load(context, R.raw.pressdown, 1);
                        TypedArray temp = getResources().obtainTypedArray(selectedarray);
                        selected = (selected+1)%(temp.length());
                        temp.recycle();

                        showView();
                    }
                });
                findViewById(R.id.left_arrow).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //if(!isplaying)soundPool.load(context, R.raw.pressdown, 1);
                        TypedArray temp = getResources().obtainTypedArray(selectedarray);
                        selected = (selected+temp.length()-1)%temp.length();
                        temp.recycle();
                        showView();
                    }
                });

                findViewById(R.id.library_info_popup).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showLibraryPopup();
                    }
                });

                setView(0);

                frmlayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });



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
        }
        showView();
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

        boolean animate = false;
        int backgroundAnimation = R.drawable.missing_content;
        int monstertitle = R.string.missing_title;
        int monstertext = R.string.missing_description;
        //normally defvalue is false but we loaded it as true for demo
        //settings.getBoolean(String.valueOf(viewarray.getResourceId(selected, R.array.missing_content)),true)
        for(UnlockedMonster unlockedMonster : unlockedMonsterList){
            if(unlockedMonster.getMonsterarrayid() == viewarray.getResourceId(selected, R.array.missing_content)){
                if(unlockedMonster.isUnlocked()){
                    @StyleableRes int tempid = 4;
                    backgroundAnimation = array.getResourceId(tempid,R.drawable.egg_idle);
                    monstertitle = array.getResourceId(tempid-2,R.drawable.egg_idle);
                    animate = true;
                }
                if(unlockedMonster.isDiscovered()){
                    @StyleableRes int tempid = 4;
                    monstertext = array.getResourceId(tempid-1,R.drawable.egg_idle);
                }
            }
        }
//        if(settings.getBoolean(String.valueOf(viewarray.getResourceId(selected, R.array.missing_content)),true)){
//            @StyleableRes int tempid = 4;
//            backgroundAnimation = array.getResourceId(tempid,R.drawable.egg_idle);
//            monstertitle = array.getResourceId(tempid-2,R.drawable.egg_idle);
//            monstertext = array.getResourceId(tempid-1,R.drawable.egg_idle);
//            animate = true;
//        }


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

    //TODO add popup selector for found monsters

    /**
     * shows the library popup, which can show specific monster info when tapped
     */
    private void showLibraryPopup(){

    }

}
