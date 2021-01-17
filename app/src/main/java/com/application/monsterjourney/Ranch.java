package com.application.monsterjourney;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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

public class Ranch extends AppCompatActivity {
    public static final String PREFS_NAME = "MyJourneyFile";
    private ImageView imageView;
    private View eggconfirmView;

    private Button backbutton;

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
        final View home = aboutinflater.inflate(R.layout.home_screen, (ViewGroup)null);
        boolean isbought = settings.getBoolean("isbought", false);
        AdView mAdView = findViewById(R.id.adView);
        Activity mainActivity = this;

        frmlayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                frmlayout.addView(home,0);

                imageView = home.findViewById(R.id.monster_icon);
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

    public void showView(){
        TextView monstercount = findViewById(R.id.partycount);
        String partycount = monsterList.size() + "/5";
        findViewById(R.id.layout2).setVisibility(View.GONE);
        findViewById(R.id.layout3).setVisibility(View.GONE);
        findViewById(R.id.layout4).setVisibility(View.GONE);
        findViewById(R.id.layout5).setVisibility(View.GONE);
        monstercount.setText(partycount);
        if(monsterList.size() < 5){
            findViewById(R.id.newegg).setVisibility(View.VISIBLE);
        }
        findViewById(R.id.selector1).setEnabled(false);
        findViewById(R.id.delete1).setEnabled(false);
        int selectedindex = 0;
        @StyleableRes int index = 4;

        for(Monster monster: monsterList){
            int currentarrayid = monster.getArrayid();
            String monstername = monster.getName();
            TypedArray array = getBaseContext().getResources().obtainTypedArray(currentarrayid);
            int resource = array.getResourceId(index,R.drawable.egg_idle);
            switch (selectedindex){
                case 0:
                    ImageView monsterimage = findViewById(R.id.monster1);
                    monsterimage.setImageDrawable(ContextCompat.getDrawable(this,resource));
                    TextView textView = findViewById(R.id.name1);
                    textView.setText(monstername);
                    break;
                case 1:
                    findViewById(R.id.layout2).setVisibility(View.VISIBLE);
                    ImageView monsterimage2 = findViewById(R.id.monster2);
                    monsterimage2.setImageDrawable(ContextCompat.getDrawable(this,resource));
                    TextView textView2 = findViewById(R.id.name2);
                    textView2.setText(monstername);
                    break;
                case 2:
                    findViewById(R.id.layout3).setVisibility(View.VISIBLE);
                    ImageView monsterimage3 = findViewById(R.id.monster3);
                    monsterimage3.setImageDrawable(ContextCompat.getDrawable(this,resource));
                    TextView textView3 = findViewById(R.id.name3);
                    textView3.setText(monstername);
                    break;
                case 4:
                    findViewById(R.id.layout4).setVisibility(View.VISIBLE);
                    ImageView monsterimage4 = findViewById(R.id.monster4);
                    monsterimage4.setImageDrawable(ContextCompat.getDrawable(this,resource));
                    TextView textView4 = findViewById(R.id.name4);
                    textView4.setText(monstername);
                    break;
                case 5:
                    findViewById(R.id.layout5).setVisibility(View.VISIBLE);
                    ImageView monsterimage5 = findViewById(R.id.monster5);
                    monsterimage5.setImageDrawable(ContextCompat.getDrawable(this,resource));
                    TextView textView5 = findViewById(R.id.name5);
                    textView5.setText(monstername);
                    break;
            }
            array.recycle();
            selectedindex++;
        }
    }


    /**
     *
     * @param view the new egg button clicked
     */
    public void newMonster(View view){
        if(eggconfirmView != null){
            return;
        }
        //view.setEnabled(false);
        LayoutInflater confirminflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert confirminflater != null;
        View confirmView = confirminflater.inflate(R.layout.confirm_popup, null);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow confirmWindow = new PopupWindow(confirmView, width2, height2, true);
        confirmWindow.setOutsideTouchable(false);
        confirmWindow.setOnDismissListener(()->eggconfirmView = null);

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
            SelectNewEgg runner = new SelectNewEgg(this);
            runner.execute();
        } );

        TextView message = confirmView.findViewById(R.id.confimation_text);
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
            if(!monsterList.get(0).getHatched()){
                Journey tempjourney = db.journeyDao().getJourney().get(0);
                tempjourney.setEventsteps(monsterList.get(0).getEvolvesteps());
                if(monsterList.get(0).getEvolvesteps() > 0){
                    tempjourney.setEventreached(false);
                }
                db.journeyDao().update(tempjourney);
            }
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
