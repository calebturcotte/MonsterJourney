package com.application.monsterjourney;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ClipDrawable;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.StyleableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Communication extends AppCompatActivity {
    /**
     * Our communication menu and class for communication with nearby apps using the nearby connections api
     */

    private static final String[] REQUIRED_PERMISSIONS =
            new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    private Strategy STRATEGY = Strategy.P2P_STAR;

    private TextView title;
    private TextView description;

    private int selectedtask;
    private int maxtasks = 3;

    private String currentsearchid;

    private int enemyarrayid = 0;
    private int enemyhealth, enemymaxhealth, battleresultInt;
    private boolean isPlayer1, battlestarted;
    private int myMatchChoice = 0;
    private int theirMatchChoice = 0;


    private String codeName = CodeNameGenerator.generate();

    private Monster currentmonster;

    private ConnectionsClient connectionsClient;

    private enum GameChoice {
        ROCK,
        PAPER,
        SCISSORS;

        boolean beats(GameChoice other) {
            return (this == GameChoice.ROCK && other == GameChoice.SCISSORS)
                    || (this == GameChoice.SCISSORS && other == GameChoice.PAPER)
                    || (this == GameChoice.PAPER && other == GameChoice.ROCK);
        }
    }

    //stuff for Rock Paper Scissors
    private int opponentScore;
    private GameChoice opponentChoice;

    private int myScore;
    private GameChoice myChoice;
    private Button rockButton;
    private Button paperButton;
    private Button scissorsButton;
    private View rockView, matchView;
    private PopupWindow rockWindow, matchWindow;
    private TextView statusText;
    private boolean isplaying;
    private MediaPlayer music;

    private String opponentEndpointId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        AsyncTask.execute(() -> {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            currentmonster = db.journeyDao().getMonster().get(0);
        });

        title = findViewById(R.id.Title);
        description = findViewById(R.id.content);

        ImageView rightscroll = findViewById(R.id.right_arrow);
        ImageView leftscroll = findViewById(R.id.left_arrow);
        Animation leftscrollanimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.leftarrow);

        Animation rightscrollanimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rightarrow);
        leftscroll.startAnimation(rightscrollanimation);
        rightscroll.startAnimation(leftscrollanimation);

        selectedtask = 0;
        shownDescription();

        findViewById(R.id.right_arrow).setOnClickListener(v -> {
            //if(!isplaying)soundPool.load(context, R.raw.pressdown, 1);
            selectedtask = (selectedtask+1)%maxtasks;
            shownDescription();

        });
        findViewById(R.id.left_arrow).setOnClickListener(v -> {
            //if(!isplaying)soundPool.load(context, R.raw.pressdown, 1);
            selectedtask = (selectedtask + maxtasks -1)%maxtasks;
            shownDescription();

        });

        //add our home screen with the current monster
        final FrameLayout homelayout = findViewById(R.id.placeholder);
        LayoutInflater homeinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        assert homeinflater != null;
        final View monsterview = homeinflater.inflate(R.layout.home_screen, (ViewGroup)null);
        Fade mFade = new Fade(Fade.IN);
        TransitionManager.beginDelayedTransition(homelayout, mFade);
        Activity myActivity = this;
        homelayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                homelayout.addView(monsterview,0);

                DisplayMonster runner = new DisplayMonster(myActivity);
                runner.execute();
                homelayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        findViewById(R.id.back).setOnClickListener(v -> {
            music.release();
            finish();
        } );

        findViewById(R.id.picker).setOnClickListener(v -> startTask());

        connectionsClient = Nearby.getConnectionsClient(this);
        resetGame(null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //check if we have permission to use connections on the phone, these must be enabled before we start searching for connections
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
            }
        }
    }

    @Override
    protected void onStop() {
        connectionsClient.stopAllEndpoints();
        super.onStop();
    }

    /** Returns true if the app was granted all the permissions. Otherwise, returns false. */
    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /** Handles user acceptance (or denial) of our permission request. */
    @CallSuper
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                //Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG).show();
                music.release();
                finish();
                return;
            }
        }
        recreate();
    }

    private void shownDescription(){
        ImageView selector1 =  findViewById(R.id.selector1);
        ImageView selector2 =  findViewById(R.id.selector2);
        ImageView selector3 =  findViewById(R.id.selector3);
        selector1.setImageResource(R.drawable.ic_selection_off);
        selector2.setImageResource(R.drawable.ic_selection_off);
        selector3.setImageResource(R.drawable.ic_selection_off);

        int tasktitle = R.string.missing_title;
        int tasktext = R.string.missing_description;
        switch(selectedtask){
            case 0:
                tasktitle = R.string.BattleFriend;
                tasktext = R.string.BattleFriendInfo;
                selector1.setImageResource(R.drawable.ic_selection_on);
                break;
            case 1:
                tasktitle = R.string.BreedFriend;
                tasktext = R.string.BreedingFriendInfo;
                selector2.setImageResource(R.drawable.ic_selection_on);
                break;
            case 2:
                tasktitle = R.string.RockFriend;
                tasktext = R.string.RockDescription;
                selector3.setImageResource(R.drawable.ic_selection_on);
                break;
        }

        title.setText(getText(tasktitle));
        description.setText(getText(tasktext));
    }

    /**
     * send out communication for specified task
     */
    private void startTask(){
        if(currentmonster == null){
            return;
        }
        isPlayer1 = true;
        findViewById(R.id.right_arrow).setEnabled(false);
        findViewById(R.id.left_arrow).setEnabled(false);
        findViewById(R.id.picker).setEnabled(false);
        title.setText(R.string.searching);
        description.setText(getText(R.string.searchmessage));
        switch(selectedtask){
            case 0: //Battle
                //Toast.makeText(this,"Connecting", Toast.LENGTH_SHORT).show();
                battlePlayer();
                break;
            case 1:
                int arrayid = currentmonster.getArrayid();
                TypedArray array = getApplicationContext().getResources().obtainTypedArray(arrayid);
                int[] monsterresources = getApplicationContext().getResources().getIntArray(arrayid);
                int stage = monsterresources[0];
                array.recycle();
                if(stage >= 3){
                    breedFriend();
                }else{
                    Toast.makeText(this,"Evolve your monster to the adult stage first!", Toast.LENGTH_SHORT).show();
                    findViewById(R.id.right_arrow).setEnabled(true);
                    findViewById(R.id.left_arrow).setEnabled(true);
                    findViewById(R.id.picker).setEnabled(true);
                    shownDescription();
                }
                break;
            case 2: // Rock Paper Scissors
                rockPaper();
                break;
        }
    }

    /**
     * initializes the matchmaking so the player can choose to accept or not
     */
    private void initializeMatchmaking(){

        FrameLayout frmlayout = findViewById(R.id.placeholder);
        LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        assert aboutinflater != null;
        final View match = aboutinflater.inflate(R.layout.matchmaking_screen, (ViewGroup)null);

        @StyleableRes int index = 4;
        match.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_background_communications));
        Fade mFade = new Fade(Fade.IN);
        TransitionManager.beginDelayedTransition(frmlayout, mFade);
        frmlayout.removeAllViews();
        frmlayout.addView(match,0);
        match.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //animate the icon for out suitor
                ImageView suitorimageView = match.findViewById(R.id.suitor_icon);
                TypedArray suitorarray = getApplicationContext().getResources().obtainTypedArray(enemyarrayid);
                int suitorresource = suitorarray.getResourceId(index,R.drawable.egg_idle);
                suitorarray.recycle();
                suitorimageView.setBackgroundResource(suitorresource);
                AnimationDrawable suitortemp = (AnimationDrawable)suitorimageView.getBackground();
                suitortemp.start();

                ValueAnimator monsterwalk = ValueAnimator.ofFloat(0.0f,1.0f);
                monsterwalk.setInterpolator(new LinearInterpolator());
                monsterwalk.setDuration(2000L);

                ImageView monster = match.findViewById(R.id.monster_icon);
                //use our r.array id to find array for current monster
                TypedArray array = getBaseContext().getResources().obtainTypedArray(currentmonster.getArrayid());
                int resource = array.getResourceId(index,R.drawable.egg_idle);
                array.recycle();
                monster.setBackgroundResource(resource);
                AnimationDrawable temp = (AnimationDrawable)monster.getBackground();
                temp.start();

                monster.setVisibility(View.INVISIBLE);
                suitorimageView.setVisibility(View.INVISIBLE);
                monsterwalk.addUpdateListener(animation -> {
                    monster.setVisibility(View.VISIBLE);
                    suitorimageView.setVisibility(View.VISIBLE);
                    monster.setScaleX(1);
                    final float progress = (float) animation.getAnimatedValue();
                    float width = match.getWidth()*progress;
                    monster.setTranslationX(-width+match.getWidth()*1.3f);
                    suitorimageView.setTranslationX(width-match.getWidth()*1.3f);
                });
                monsterwalk.start();
                matchmaker_popup();
                match.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    /**
     * Create popup where we decide if we want to match with the monster connected with or not
     */
    private void matchmaker_popup(){
        if(matchView != null){
            return;
        }
       // AtomicBoolean accepted = new AtomicBoolean(false);
        LayoutInflater confirminflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert confirminflater != null;
        matchView = confirminflater.inflate(R.layout.matchmaker_popup, null);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        matchWindow = new PopupWindow(matchView, width2, height2);
        matchWindow.setOutsideTouchable(false);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            matchWindow.setElevation(20);
        }
        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        matchWindow.setAnimationStyle(R.style.PopupAnimation);
        matchWindow.showAtLocation(findViewById(R.id.placeholder), Gravity.CENTER, 0, 0);
        matchView.findViewById(R.id.close).setVisibility(View.INVISIBLE);
        matchWindow.setOnDismissListener(() -> {
            FrameLayout frmlayout = findViewById(R.id.placeholder);
            LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            assert aboutinflater != null;
            final View home = aboutinflater.inflate(R.layout.home_screen, (ViewGroup)null);
            Fade mFade = new Fade(Fade.IN);
            TransitionManager.beginDelayedTransition(frmlayout, mFade);
            frmlayout.removeAllViews();
            frmlayout.addView(home,0);
            home.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    selectedIcon(currentmonster.getArrayid());
                    home.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
                    matchView = null;
        });

        ImageView tempmatchmaker = matchView.findViewById(R.id.matchmaker_popup_icon);

        tempmatchmaker.setBackgroundResource(R.drawable.matchmaker_idle);
        AnimationDrawable matchanimator = (AnimationDrawable) tempmatchmaker.getBackground();
        matchanimator.start();

        TextView matchtext = matchView.findViewById(R.id.confimation_text);
        matchtext.setText(getText(R.string.BreedingFriendMessage));
        Button confirmbutton = matchView.findViewById(R.id.confirm);
        Button backbutton = matchView.findViewById(R.id.back);

        confirmbutton.setOnClickListener(v -> {
            myMatchChoice = 2;
            byte[] bytes = ByteBuffer.allocate(4).putInt(myMatchChoice).array();
            connectionsClient.sendPayload(
                    opponentEndpointId, Payload.fromBytes(bytes));
            backbutton.setEnabled(false);
            confirmbutton.setEnabled(false);
        });
        backbutton.setOnClickListener(v -> {
            myMatchChoice = 1;
            byte[] bytes = ByteBuffer.allocate(4).putInt(myMatchChoice).array();
            connectionsClient.sendPayload(
                    opponentEndpointId, Payload.fromBytes(bytes));
            backbutton.setEnabled(false);
            confirmbutton.setEnabled(false);
        });
    }

    /**
     * retire our current monster and hatch the new egg, create a new one instead if there is still space in your party
     */
    private void obtainedNewEgg(){
        TypedArray eggArray = getResources().obtainTypedArray(enemyarrayid);
        @StyleableRes int index = 12;
        int[] monsterresources = getResources().getIntArray(currentmonster.getArrayid());
        int evolutions = monsterresources[1];
        int eggid = eggArray.getResourceId(index+evolutions, R.array.enigma_egg);
        eggArray.recycle();
        ImageView hearts = findViewById(R.id.matched_event);
        hearts.setVisibility(View.VISIBLE);
        final Animation eventanimation = new AlphaAnimation(1, 0); //to change visibility from visible to invisible
        eventanimation.setDuration(1000); //1 second duration for each animation cycle
        eventanimation.setInterpolator(new LinearInterpolator());
        eventanimation.setRepeatCount(Animation.INFINITE); //repeating indefinitely
        eventanimation.setRepeatMode(Animation.REVERSE); //animation will start from end point once ended.
        hearts.startAnimation(eventanimation); //to start animation
        Handler h = new Handler();
        //Run a runnable to hide food after it has been eaten
        h.postDelayed(()->{
            matchWindow.dismiss();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            //where right side is current view
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            music.release();
            finish();
        }, 3000);
        //TODO breed/egg animation
        AsyncTask.execute(() -> {
            AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
            // run your queries here!
            Journey temp = db.journeyDao().getJourney().get(0);
            temp.setEventsteps(100);
            temp.setEventtype(0);
            temp.setEventreached(false);
            db.journeyDao().update(temp);

            //retire the current monster and
            Monster tempmonster = db.journeyDao().getMonster().get(0);
            List<Monster> monsterList = db.journeyDao().getMonster();
            if(monsterList.size() < 5){
                db.journeyDao().insertMonster(Monster.populateData().updateMonster(tempmonster));
            }
            History temphistory = new History(tempmonster.getGeneration(), tempmonster.getArrayid(), tempmonster.getName());
            db.journeyDao().insertHistory(temphistory);

            tempmonster.newEgg(eggid);
            List<UnlockedMonster> unlockedMonsters = db.journeyDao().getUnlockedMonster();
            for(UnlockedMonster unlockedMonster : unlockedMonsters){
                if(unlockedMonster.getMonsterarrayid() == eggid){
                    unlockedMonster.setDiscovered(true);
                    unlockedMonster.setUnlocked(true);
                    break;
                }
            }
            db.journeyDao().updateUnlockedMonster(unlockedMonsters);
            db.journeyDao().updateMonster(tempmonster);
        });
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
        imageView.setImageDrawable(ContextCompat.getDrawable(this, resource));
        findViewById(R.id.back_screen).setBackgroundResource(R.drawable.ic_background_communications);

        AnimationDrawable monsteranimator = (AnimationDrawable) imageView.getDrawable();
        monsteranimator.start();

    }

    /**
     * battle nearby player, both devices will advertise and detect until a connection is established
     */
    private void battlePlayer(){
        currentsearchid = "BattleID";
        battleresultInt = 0;
        battlestarted = false;
        startAdvertising();
        startDiscovery();
    }

    private void breedFriend(){
        currentsearchid = "BreedID";
        myMatchChoice = 0;
        theirMatchChoice = 0;
        startAdvertising();
        startDiscovery();
    }

    /**
     * our task that starts the rockpaperscissors game connection
     */
    private void rockPaper(){
        currentsearchid = "RockID";
        startAdvertising();
        startDiscovery();
    }

    /**
     * google nearby connections example code, used for advertising a connection
     */
    private void startAdvertising() {
        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();
        switch(selectedtask){
            case 2:
                connectionsClient
                        .startAdvertising(
                                codeName, getPackageName()+currentsearchid, connectionLifecycleCallback, advertisingOptions)
                        .addOnSuccessListener(
                                (Void unused) -> {
                                })
                        .addOnFailureListener(
                                (Exception e) -> Toast.makeText(this,"Unable to advertise", Toast.LENGTH_SHORT).show());
                break;
            case 1:
                connectionsClient
                        .startAdvertising(
                                codeName, getPackageName()+currentsearchid, matchconnectionLifecycleCallback, advertisingOptions)
                        .addOnFailureListener(
                                (Exception e) -> Toast.makeText(this,"Unable to advertise", Toast.LENGTH_SHORT).show());
                break;
            case 0:
                connectionsClient
                        .startAdvertising(
                                codeName, getPackageName()+currentsearchid, battleconnectionLifecycleCallback, advertisingOptions)
                        .addOnFailureListener(
                                (Exception e) -> Toast.makeText(this,"Unable to advertise", Toast.LENGTH_SHORT).show());
                break;
        }

    }

    /**
     * discovery listener
     */
    private void startDiscovery() {
        DiscoveryOptions discoveryOptions =
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();
        switch(selectedtask){
            case 2:
                connectionsClient
                        .startDiscovery(getPackageName()+currentsearchid, endpointDiscoveryCallback, discoveryOptions)
                        .addOnSuccessListener(
                                (Void unused) -> {
                                })
                        .addOnFailureListener(
                                (Exception e) -> Toast.makeText(this,"Unable to discover", Toast.LENGTH_SHORT).show());
                break;
            case 1:
                connectionsClient
                        .startDiscovery(getPackageName()+currentsearchid, matchendpointDiscoveryCallback, discoveryOptions)
                        .addOnFailureListener(
                                (Exception e) -> Toast.makeText(this,"Unable to discover", Toast.LENGTH_SHORT).show());
                break;
            case 0:
                connectionsClient
                        .startDiscovery(getPackageName()+currentsearchid, battleendpointDiscoveryCallback, discoveryOptions)
                        .addOnFailureListener(
                                (Exception e) -> Toast.makeText(this,"Unable to discover", Toast.LENGTH_SHORT).show());
                break;
        }
    }

    /**
     * callback for the discovery
     */
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    connectionsClient
                            .requestConnection(codeName, endpointId, connectionLifecycleCallback)
                            .addOnSuccessListener(
                                    (Void unused) -> {
                                        // We successfully requested a connection. Now both sides
                                        // must accept before the connection is established.
                                    })
                            .addOnFailureListener(
                                    (Exception e) -> {
                                        // Nearby Connections failed to request the connection.
                                    });
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    // A previously discovered endpoint has gone away.
                }
            };

    /**
     * callback for the discovery for battle activity
     */
    private final EndpointDiscoveryCallback battleendpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    connectionsClient
                            .requestConnection(codeName, endpointId, battleconnectionLifecycleCallback)
                            .addOnSuccessListener(
                                    (Void unused) -> {
                                        isPlayer1 = false;
                                        // We successfully requested a connection. Now both sides
                                        // must accept before the connection is established.
                                    })
                            .addOnFailureListener(
                                    (Exception e) -> {
                                        // Nearby Connections failed to request the connection.
                                    });
                }
                @Override
                public void onEndpointLost(String endpointId) {
                    // A previously discovered endpoint has gone away.
                }
            };

    /**
     * callback for the discovery for battle activity
     */
    private final EndpointDiscoveryCallback matchendpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    connectionsClient
                            .requestConnection(codeName, endpointId, matchconnectionLifecycleCallback)
                            .addOnSuccessListener(
                                    (Void unused) -> {
                                        isPlayer1 = false;
                                        // We successfully requested a connection. Now both sides
                                        // must accept before the connection is established.
                                    })
                            .addOnFailureListener(
                                    (Exception e) -> {
                                        // Nearby Connections failed to request the connection.
                                    });
                }
                @Override
                public void onEndpointLost(String endpointId) {
                    // A previously discovered endpoint has gone away.
                }
            };

    /**
     * lifecycle for our connection, used in rockpaper scissors
     */
    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    // Automatically accept the connection on both sides.
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if(result.getStatus().isSuccess()){
                        // We're connected! Can now start sending and receiving data.
                        connectionsClient.stopDiscovery();
                        connectionsClient.stopAdvertising();
                        battleSetup();
                        //initializeRockPaperScissors();
                        opponentEndpointId = endpointId; // the id of the device we are connected to
                        //done after so endpointid is not overwritten
                    }
                    else{
                        Toast.makeText(getApplicationContext(),"Connection Failed", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                    resetGame(null);
                }
            };

    //connectionlifecyclecallback for our battle
    private final ConnectionLifecycleCallback battleconnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    connectionsClient.acceptConnection(endpointId, battlepayloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        // We're connected! Can now start sending and receiving data.
                        connectionsClient.stopDiscovery();
                        connectionsClient.stopAdvertising();
                        opponentEndpointId = endpointId; // the id of the device we are connected to
                        //done after so endpointid is not overwritten
                        battleSetup();
                    } else {
                        Toast.makeText(getApplicationContext(), "Connection Failed", Toast.LENGTH_SHORT).show();
                    }
                }


                @Override
                public void onDisconnected(String endpointId) {
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                    resetGame(null);
                }
            };

    private final ConnectionLifecycleCallback matchconnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    connectionsClient.acceptConnection(endpointId, matchpayloadCallback);
                    Toast.makeText(getApplicationContext(),"Connection Success", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
//                        Toast.makeText(getApplicationContext(), String.valueOf(endpointId), Toast.LENGTH_SHORT).show();
                        // We're connected! Can now start sending and receiving data.
                        connectionsClient.stopDiscovery();
                        connectionsClient.stopAdvertising();
                        opponentEndpointId = endpointId; // the id of the device we are connected to
                        //done after so endpointid is not overwritten
                        battleSetup();
                    } else {
                        Toast.makeText(getApplicationContext(), "Connection Failed", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                    if(matchView != null){
                        matchWindow.dismiss();
                    }
                    resetGame(null);
                }
            };

    // Callbacks for receiving payloads
    // used for our rock paper scissors game
    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    if(enemyarrayid == 0){
                        enemyarrayid = ByteBuffer.wrap(Objects.requireNonNull(payload.asBytes())).getInt();
                        logDiscovery(enemyarrayid);
                    }
                    else {
                        opponentChoice = GameChoice.valueOf(new String(Objects.requireNonNull(payload.asBytes()), UTF_8));
                    }

                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                        if(enemyarrayid != 0 && rockView == null){
                            initializeRockPaperScissors();
                        }
                        else if (myChoice != null && opponentChoice != null) {
                            finishRound();
                        }
                    }
                }
            };

    //payload for our battleresults
    private final PayloadCallback battlepayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    if(enemyarrayid == 0){
                        enemyarrayid = ByteBuffer.wrap(Objects.requireNonNull(payload.asBytes())).getInt();
                        logDiscovery(enemyarrayid);
                    }
                    else if(battleresultInt == 0){
                        battleresultInt = ByteBuffer.wrap(Objects.requireNonNull(payload.asBytes())).getInt();
                    }
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                        if(!battlestarted){
                            if (isPlayer1 && enemyarrayid != 0) {
                                //logic for starting a battle
                                battleanimation();
                            }
                            else if(!isPlayer1 && enemyarrayid !=0 && battleresultInt !=0){
                                battleanimation();
                            }
                        }


                    }
                }
            };

    //payload for our matchmaking
    private final PayloadCallback matchpayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    if(enemyarrayid == 0){
                        enemyarrayid = ByteBuffer.wrap(Objects.requireNonNull(payload.asBytes())).getInt();
                        logDiscovery(enemyarrayid);
                    }
                    else if(theirMatchChoice == 0){
                        theirMatchChoice = ByteBuffer.wrap(Objects.requireNonNull(payload.asBytes())).getInt();
                    }
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                        if(enemyarrayid != 0 && matchView == null){
                            initializeMatchmaking();
                        }
                        else if(myMatchChoice != 0 && theirMatchChoice != 0){
                            if(myMatchChoice + theirMatchChoice >=4){
                                obtainedNewEgg();
                            }
                            else{
                                matchWindow.dismiss();
                            }
                            disconnect();
                        }


                    }
                }
            };

    /**
     * initialize our rock paper scissors game
     */
    private void initializeRockPaperScissors(){
        if(rockView != null){
            return;
        }
        //TODO connection doesn't initialize every time

        LayoutInflater rockinflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert rockinflater != null;
        rockView = rockinflater.inflate(R.layout.rock_paper_scissors, null);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        rockWindow = new PopupWindow(rockView, width2, height2);
        rockWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        rockWindow.setOutsideTouchable(false);


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            rockWindow.setElevation(20);
        }
        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        rockWindow.setAnimationStyle(R.style.PopupAnimation);
        rockWindow.showAtLocation(findViewById(R.id.placeholder), Gravity.CENTER, 0, 0);

        rockWindow.setOnDismissListener(() -> {
            rockView = null;
        });

        rockButton = rockView.findViewById(R.id.rock);
        scissorsButton = rockView.findViewById(R.id.scissors);
        paperButton = rockView.findViewById(R.id.paper);
        statusText = rockView.findViewById(R.id.result);

        resetGame(rockView);
        setGameChoicesEnabled(true);

    }

    /** Wipes all game state and updates the UI accordingly. */
    private void resetGame(View rockView) {
        opponentEndpointId = null;
//        opponentName = null;
        opponentChoice = null;
        opponentScore = 0;
        myChoice = null;
        myScore = 0;
        enemyarrayid = 0;
        findViewById(R.id.right_arrow).setEnabled(true);
        findViewById(R.id.left_arrow).setEnabled(true);
        findViewById(R.id.picker).setEnabled(true);
        shownDescription();

        if(rockView != null){
            updateScore(myScore, opponentScore, rockView);
        }
        //setButtonState(false);
    }


    /** Sends a {@link GameChoice} to the other player. */
    public void makeMove(View view) {
        if (view.getId() == R.id.rock) {
            sendGameChoice(GameChoice.ROCK);
        } else if (view.getId() == R.id.paper) {
            sendGameChoice(GameChoice.PAPER);
        } else if (view.getId() == R.id.scissors) {
            sendGameChoice(GameChoice.SCISSORS);
        }
    }

    /** Sends the user's selection of rock, paper, or scissors to the opponent. */
    private void sendGameChoice(GameChoice choice) {
        myChoice = choice;
        connectionsClient.sendPayload(
                opponentEndpointId, Payload.fromBytes(choice.name().getBytes(UTF_8)));

        statusText.setText(choice.name());
        // No changing your mind!
        setGameChoicesEnabled(false);
    }

    /** Enables/disables the rock, paper, and scissors buttons. */
    private void setGameChoicesEnabled(boolean enabled) {
        rockButton.setEnabled(enabled);
        paperButton.setEnabled(enabled);
        scissorsButton.setEnabled(enabled);
    }

    /** Set the current score */
    private void updateScore(int myScore, int opponentScore, View thisview){
        TextView p1score = thisview.findViewById(R.id.score1);
        p1score.setText(String.valueOf(myScore));
        TextView p2score = thisview.findViewById(R.id.score2);
        p2score.setText(String.valueOf(opponentScore));
    }

    private void setStatusText(int text){
        statusText.setText(getText(text));
    }

    /** Determines the winner and update game state/UI after both players have chosen. */
    private void finishRound() {
        if (myChoice.beats(opponentChoice)) {
            // Win!
            setStatusText(R.string.win_message);
            myScore++;
            happyAnimation(2000);
        } else if (myChoice == opponentChoice) {
            // Tie, same choice by both players
            setStatusText(R.string.tie_message);
        } else {
            // Loss
            setStatusText(R.string.loss_message);
            opponentScore++;
            angryAnimation(2000);
        }

        myChoice = null;
        opponentChoice = null;

        updateScore(myScore, opponentScore, rockView);

        if(myScore >= 3){
            setStatusText(R.string.game_win);
            Handler h = new Handler();
            //Run a runnable to hide food after it has been eaten
            h.postDelayed(() -> rockWindow.dismiss(), 1199);
            disconnect();
            return;
        }
        else if(opponentScore >= 3){
            setStatusText(R.string.game_lost);
            Handler h = new Handler();
            //Run a runnable to hide food after it has been eaten
            h.postDelayed(() -> rockWindow.dismiss(), 1199);
            disconnect();
            return;
        }
        // Ready for another round
        setGameChoicesEnabled(true);
    }


    /**
     * send the arrayid of our monster to the opponent
     */
    private void battleSetup(){
        byte[] bytes = ByteBuffer.allocate(4).putInt(currentmonster.getArrayid()).array();
        connectionsClient.sendPayload(
                opponentEndpointId, Payload.fromBytes(bytes));
    }

    /**
     * animation for our battle
     */
    private void battleanimation(){
        battlestarted = true;
        //Toast.makeText(this, String.valueOf(enemyarrayid), Toast.LENGTH_SHORT).show();
        int[] enemyfound = getResources().getIntArray(enemyarrayid);
        enemyhealth = enemyfound[5+enemyfound[1]];
        enemymaxhealth = enemyfound[5+enemyfound[1]];
        int enemyattack = enemyfound[6+enemyfound[1]];
        int enemychance = enemyfound[7+enemyfound[1]];
        currentmonster.initializebattlestats(this);
        ArrayList<Integer> battleresult = new ArrayList<>();
        if(isPlayer1){
            battleresult = currentmonster.battle(enemyattack,enemyhealth,enemychance, 0, true);
            int result = 0;
            int index = 1;
            for(Integer integer: battleresult){
                result = result + integer*index;
                index *= 10;
            }
            byte[] bytes = ByteBuffer.allocate(4).putInt(result).array();
            connectionsClient.sendPayload(
                    opponentEndpointId, Payload.fromBytes(bytes));
        }
        else{
            int length = String.valueOf(battleresultInt).length();
            for(int i = length; i > 0; i--){
                int added_value = battleresultInt%10;
                if(added_value == 1){
                    added_value = 2;
                }
                else if(added_value == 2){
                    added_value = 1;
                }
                battleresult.add(added_value);
                battleresultInt = battleresultInt / 10;
            }
        }
        performbattle(battleresult,
                enemyattack);
    }

    public void performbattle(final ArrayList<Integer> rounds, final int enemyattackvalue){
        //create popup display for monster stats durring battle
        LayoutInflater battleinflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert battleinflater != null;
        View battleView = battleinflater.inflate(R.layout.battle_info, findViewById(R.id.parent),false);
        int width2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        int height2 = ConstraintLayout.LayoutParams.MATCH_PARENT;
        //boolean focusable2 = true; // lets taps outside the popup also dismiss it
        final PopupWindow battleWindow = new PopupWindow(battleView, width2, height2);
        battleWindow.setOutsideTouchable(false);
        if(isPlayer1){
            battleWindow.setOnDismissListener(this::disconnect);
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            battleWindow.setElevation(20);
        }
        // show the battle popup window
        // which view you pass in doesn't matter, it is only used for the window token
        battleWindow.setAnimationStyle(R.style.PopupAnimation);
        battleWindow.showAtLocation(findViewById(R.id.placeholder), Gravity.CENTER, 0, 0);
        final TextView enemyhealthtext = battleView.findViewById(R.id.enemy_health_text);
        final TextView playerhealthtext = battleView.findViewById(R.id.player_health_text);
        String temphealth1 = getText(R.string.BattleEnemyHealth)+String.valueOf(enemyhealth);
        enemyhealthtext.setText(temphealth1);
        String temphealth2 = getText(R.string.BattlePlayerHealth)+String.valueOf(currentmonster.getCurrenthealth());
        playerhealthtext.setText(temphealth2);
        ImageView enemyhealthbar = battleView.findViewById(R.id.enemy_health);
        ImageView playerhealthbar = battleView.findViewById(R.id.player_health);
        final ClipDrawable enemyhealthfill = (ClipDrawable) enemyhealthbar.getDrawable();
        //max fill is 10000, or tapcount of 5
        enemyhealthfill.setLevel((int)(10000*(enemyhealth/(float)enemymaxhealth)));
        final ClipDrawable playerhealthfill = (ClipDrawable) playerhealthbar.getDrawable();
        //max fill is 10000, or tapcount of 5
        playerhealthfill.setLevel((int)(10000*(currentmonster.getCurrenthealth()/(float)currentmonster.getMaxhealth())));

        //show our animation for each attack
        final FrameLayout frmlayout = findViewById(R.id.placeholder);
        LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        assert aboutinflater != null;
        final View battle = aboutinflater.inflate(R.layout.battlescreen, null);
        Fade mFade = new Fade(Fade.IN);
        TransitionManager.beginDelayedTransition(frmlayout, mFade);

        frmlayout.removeAllViews();
        frmlayout.addView(battle,0);
        findViewById(R.id.back_screen).setBackgroundResource(R.drawable.ic_background_communications);

        final ImageView monster = battle.findViewById(R.id.monster_icon);
        final ImageView attack1View = battle.findViewById(R.id.myattack);
        final ImageView attack2View = battle.findViewById(R.id.theirattack);
        switch(enemyattackvalue){
            case 2:
                attack2View.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.attack_2));
                break;
            case 3:
                attack2View.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.attack_3));
                break;
        }

        switch (currentmonster.getPower()){
            case 2:
                attack1View.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.attack_2));
                break;
            case 3:
                attack1View.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.attack_3));
                break;
        }
        //add our different animations together to play
        AnimatorSet battleAnimation = new AnimatorSet();
        ArrayList<Animator> attackanimations = new ArrayList<>();
        //use our r.array id to find array for current monster
        @StyleableRes int index = 4;
        TypedArray array1 = getApplicationContext().getResources().obtainTypedArray(currentmonster.getArrayid());
        TypedArray array2 = getApplicationContext().getResources().obtainTypedArray(enemyarrayid);
        int[] playerrresources = getResources().getIntArray(currentmonster.getArrayid());
        int[] enemyresources = getResources().getIntArray(enemyarrayid);
        final int player1 = array1.getResourceId(index,R.drawable.egg_idle);
        final int player2 = array2.getResourceId(index,R.drawable.egg_idle);
        final int playerattack1 = array1.getResourceId(index + playerrresources[1]+9,R.drawable.egg_idle);
        final int enemyattack1 = array2.getResourceId(index + enemyresources[1]+9,R.drawable.egg_idle);
        array1.recycle();
        array2.recycle();

        for(Integer round : rounds){
            boolean damagedanimation = false;
            ValueAnimator playerattack = ValueAnimator.ofFloat(0.0f,1.0f);
            playerattack.setInterpolator(new LinearInterpolator());
            playerattack.setDuration(1000L);
            ValueAnimator player2attack = ValueAnimator.ofFloat(0.0f,1.0f);
            player2attack.setInterpolator(new LinearInterpolator());
            player2attack.setDuration(1000L);

            //the fireballs attacking
            ValueAnimator attackanimator = ValueAnimator.ofFloat(0.0f, 1.0f);
            attackanimator.setInterpolator(new LinearInterpolator());
            attackanimator.setDuration(1300L);

            ValueAnimator damaged = ValueAnimator.ofFloat(0.0f,1.0f);
            damaged.setInterpolator(new LinearInterpolator());
            damaged.setDuration(1000L);

            switch(round){
                case 0: //draw
                    playerattack.addUpdateListener(animation -> {
                        monster.setVisibility(View.VISIBLE);
                        monster.setScaleX(1);
                        final float progress = (float) animation.getAnimatedValue();

                        if(progress > 0.7f){
                            attack1View.setVisibility(View.VISIBLE);
                            float width =-(progress)* battle.getWidth();
                            attack1View.setTranslationX(width);
                            monster.setBackgroundResource(playerattack1);
                        }
                        else{
                            monster.setBackgroundResource(player1);
                            attack1View.setVisibility(View.INVISIBLE);
                            attack2View.setVisibility(View.INVISIBLE);
                        }
                    });
                    player2attack.addUpdateListener(animation -> {
                        monster.setVisibility(View.VISIBLE);
                        monster.setScaleX(-1);
                        final float progress = (float) animation.getAnimatedValue();
                        if(progress > 0.7f){
                            attack2View.setVisibility(View.VISIBLE);
                            float width =(progress)* battle.getWidth();
                            attack2View.setTranslationX(width);
                            monster.setBackgroundResource(enemyattack1);
                        }
                        else{
                            monster.setBackgroundResource(player2);
                            attack1View.setVisibility(View.INVISIBLE);
                            attack2View.setVisibility(View.INVISIBLE);
                        }
                    });
                    attackanimator.addUpdateListener(animation -> {
                        monster.setVisibility(View.INVISIBLE);
                        if(attack2View.getTranslationX() >=  (battle.getWidth()*0.5f - attack2View.getWidth())){
                            attack1View.setVisibility(View.INVISIBLE);
                            attack2View.setVisibility(View.INVISIBLE);
                        }
                        else{
                            attack1View.setVisibility(View.VISIBLE);
                            attack2View.setVisibility(View.VISIBLE);
                        }
                        final float progress = (float) animation.getAnimatedValue();
                        float width = battle.getWidth()*progress;
                        attack1View.setTranslationX(-width);
                        attack2View.setTranslationX(width);
                    });


                    break;
                case 1: //p1 win
                    playerattack.addUpdateListener(animation -> {
                        monster.setVisibility(View.VISIBLE);
                        monster.setScaleX(1);
                        final float progress = (float) animation.getAnimatedValue();
                        if(progress > 0.7f){
                            attack1View.setVisibility(View.VISIBLE);
                            float width =-(progress)* battle.getWidth();
                            attack1View.setTranslationX(width);
                            monster.setBackgroundResource(playerattack1);
                        }
                        else{
                            monster.setBackgroundResource(player1);
                            attack1View.setVisibility(View.INVISIBLE);
                            attack2View.setVisibility(View.INVISIBLE);
                        }
                    });
                    player2attack.addUpdateListener(animation -> {
                        monster.setVisibility(View.VISIBLE);
                        monster.setScaleX(-1);
                        monster.setBackgroundResource(player2);
                        attack1View.setVisibility(View.INVISIBLE);
                        attack2View.setVisibility(View.INVISIBLE);
                    });
                    attackanimator.addUpdateListener(animation -> {
                        monster.setVisibility(View.INVISIBLE);
                        attack1View.setVisibility(View.VISIBLE);
                        attack2View.setVisibility(View.INVISIBLE);
                        final float progress = (float) animation.getAnimatedValue();
                        float width = battle.getWidth()*progress;
                        attack1View.setTranslationX(-width);
                        attack2View.setTranslationX(width);
                    });
                    damaged.addUpdateListener(animation -> {
                        monster.setVisibility(View.VISIBLE);
                        monster.setScaleX(-1);
                        final float progress = (float) animation.getAnimatedValue();

                        if(progress < 0.4f){
                            monster.setBackgroundResource(player2);
                            attack1View.setVisibility(View.VISIBLE);
                            float width =-progress* battle.getWidth();
                            attack1View.setTranslationX(width);
                        }
                        else{
                            monster.setBackgroundResource(R.drawable.damaged);
                            AnimationDrawable tempanimator = (AnimationDrawable) monster.getBackground();
                            tempanimator.start();
                            attack1View.setVisibility(View.INVISIBLE);
                            attack2View.setVisibility(View.INVISIBLE);
                        }
                    });
                    damaged.addListener(new AnimatorListenerAdapter()
                    {
                        @Override
                        public void onAnimationEnd(Animator animation)
                        {
                            enemyhealth = enemyhealth - currentmonster.getPower();
                            String temphealth = getText(R.string.BattleEnemyHealth)+String.valueOf(enemyhealth);
                            enemyhealthtext.setText(temphealth);
                            enemyhealthfill.setLevel((int)((10000L*enemyhealth)/enemymaxhealth));
                        }
                    });
                    damagedanimation = true;
                    break;
                case 2: //p2 win
                    playerattack.addUpdateListener(animation -> {
                        monster.setVisibility(View.VISIBLE);
                        monster.setScaleX(1);
                        monster.setBackgroundResource(player1);
                        attack1View.setVisibility(View.INVISIBLE);
                        attack2View.setVisibility(View.INVISIBLE);
                    });
                    player2attack.addUpdateListener(animation -> {
                        monster.setVisibility(View.VISIBLE);
                        monster.setScaleX(-1);
                        final float progress = (float) animation.getAnimatedValue();
                        if(progress > 0.7f){
                            attack2View.setVisibility(View.VISIBLE);
                            float width =(progress)* battle.getWidth();
                            attack2View.setTranslationX(width);
                            monster.setBackgroundResource(enemyattack1);
                        }
                        else{
                            monster.setBackgroundResource(player2);
                            attack1View.setVisibility(View.INVISIBLE);
                            attack2View.setVisibility(View.INVISIBLE);
                        }
                    });
                    attackanimator.addUpdateListener(animation -> {
                        monster.setVisibility(View.INVISIBLE);
                        attack1View.setVisibility(View.INVISIBLE);
                        attack2View.setVisibility(View.VISIBLE);
                        final float progress = (float) animation.getAnimatedValue();
                        float width = battle.getWidth()*progress;
                        attack1View.setTranslationX(-width);
                        attack2View.setTranslationX(width);
                    });
                    damaged.addUpdateListener(animation -> {
                        monster.setVisibility(View.VISIBLE);
                        monster.setScaleX(1);
                        final float progress = (float) animation.getAnimatedValue();

                        if(progress < 0.4f){
                            monster.setBackgroundResource(player1);
                            attack2View.setVisibility(View.VISIBLE);
                            float width =progress* battle.getWidth();
                            attack2View.setTranslationX(width);
                        }
                        else{
                            monster.setBackgroundResource(R.drawable.damaged);
                            AnimationDrawable tempanimator = (AnimationDrawable) monster.getBackground();
                            tempanimator.start();
                            attack1View.setVisibility(View.INVISIBLE);
                            attack2View.setVisibility(View.INVISIBLE);
                        }
                    });
                    damaged.addListener(new AnimatorListenerAdapter()
                    {
                        @Override
                        public void onAnimationEnd(Animator animation)
                        {
                            currentmonster.setCurrenthealth(currentmonster.getCurrenthealth()-enemyattackvalue);
                            String temphealth = getText(R.string.BattlePlayerHealth)+String.valueOf(currentmonster.getCurrenthealth());
                            playerhealthtext.setText(temphealth);
                            playerhealthfill.setLevel((int)(10000L*currentmonster.getCurrenthealth())/currentmonster.getMaxhealth());
                        }
                    });
                    damagedanimation = true;
                    break;
            }

            attackanimations.add(playerattack);
            attackanimations.add(player2attack);
            attackanimations.add(attackanimator);
            if(damagedanimation)attackanimations.add(damaged);

        }
        battleAnimation.playSequentially(attackanimations);
        battleAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                //check if the battle has ended
                battleWindow.dismiss();
                frmlayout.removeAllViews();
                LayoutInflater aboutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                assert aboutinflater != null;
                final View home = aboutinflater.inflate(R.layout.home_screen, (ViewGroup)null);
                Fade mFade = new Fade(Fade.IN);
                TransitionManager.beginDelayedTransition(frmlayout, mFade);

                frmlayout.addView(home);

                home.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if(enemyhealth > 0){
                            angryAnimation(2000);
                        }
                        else {
                            happyAnimation(2000);
                        }
                        home.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        //play our animation for this battle
        battleAnimation.start();
    }

    /** Disconnects from the opponent and reset the UI. */
    public void disconnect() {
        connectionsClient.disconnectFromEndpoint(opponentEndpointId);
        resetGame(null);
    }

    /**
     * log the arrayid we found in our discovered list
     * @param enemyarrayid the arrayid of the enemy we found
     */
    public void logDiscovery(int enemyarrayid){
        AsyncTask.execute(()->{
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            List<UnlockedMonster> unlockedMonsterList = db.journeyDao().getUnlockedMonster();
            for(UnlockedMonster unlockedMonster : unlockedMonsterList){
                if(unlockedMonster.getMonsterarrayid() == enemyarrayid){
                    unlockedMonster.setDiscovered(true);
                    break;
                }
            }
            db.journeyDao().updateUnlockedMonster(unlockedMonsterList);
        });

    }

    /**
     * play happy animation for selected amount of time
     */
    public void happyAnimation(int duration){
        TypedArray array = getBaseContext().getResources().obtainTypedArray(currentmonster.getArrayid());
        @StyleableRes int index = 4;
        int[] monsterresources = getResources().getIntArray(currentmonster.getArrayid());
        int evolutions = monsterresources[1];
        int resource = array.getResourceId(index+evolutions+10, R.drawable.egg_idle);
        array.recycle();
        ImageView imageView = findViewById(R.id.monster_icon);
        imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), resource));
        AnimationDrawable monsteranimator = (AnimationDrawable) imageView.getDrawable();
        monsteranimator.start();
        Handler h = new Handler();
        h.postDelayed(() -> {
            monsteranimator.stop();
            selectedIcon(currentmonster.getArrayid());
        }, duration);
    }

    /**
     * play angry animation for selected amount of time
     */
    public void angryAnimation(int duration){
        TypedArray array = getBaseContext().getResources().obtainTypedArray(currentmonster.getArrayid());
        @StyleableRes int index = 4;
        int[] monsterresources = getResources().getIntArray(currentmonster.getArrayid());
        int evolutions = monsterresources[1];
        int resource = array.getResourceId(index+evolutions+9, R.drawable.egg_idle);
        array.recycle();
        ImageView imageView = findViewById(R.id.monster_icon);
        imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), resource));
        AnimationDrawable monsteranimator = (AnimationDrawable) imageView.getDrawable();
        monsteranimator.start();
        Handler h = new Handler();
        h.postDelayed(() -> {
            monsteranimator.stop();
            selectedIcon(currentmonster.getArrayid());
        }, duration);
    }

    /**
     * override our back pressed button so we don't accidentally close any popup windows
     */
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }

    static private class DisplayMonster extends AsyncTask<String,TextView,String> {
        private int arrayid;
        // Weak references will still allow the Activity to be garbage-collected
        private final WeakReference<Activity> weakActivity;

        public DisplayMonster(Activity myActivity){
            this.weakActivity = new WeakReference<>(myActivity);
        }
        @Override
        protected String doInBackground(String... strings) {
            AppDatabase db = AppDatabase.getInstance(weakActivity.get());
            arrayid = db.journeyDao().getMonster().get(0).getArrayid();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Communication activity = (Communication) weakActivity.get();
            activity.selectedIcon(arrayid);

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        int tempvolume = 80;
        music = MediaPlayer.create(Communication.this,R.raw.communications);
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
}
