package com.application.monsterjourney;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.Toast;

import androidx.annotation.StyleableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.nio.ByteBuffer;
import java.util.ArrayList;

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
            };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    private TextView title;
    private TextView description;

    private int selectedtask;
    private int maxtasks = 2;

    private String currentsearchid;

    private int enemyarrayid = 0;
    private boolean isPlayer1;

    private Monster currentmonster;
    private int trainingtapcount = 0;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
                currentmonster = db.journeyDao().getMonster().get(0);
            }
        });

//        AsyncTask.execute(new Runnable() {
//            @Override
//            public void run() {
//                AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
//                //Temp test
//                Item tempitem = new Item(1);
//                Item tempitem2 = new Item(2);
//                db.journeyDao().insertItem(tempitem);
//                db.journeyDao().insertItem(tempitem2);
//            }
//        });

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

        findViewById(R.id.right_arrow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if(!isplaying)soundPool.load(context, R.raw.pressdown, 1);
                selectedtask = (selectedtask+1)%maxtasks;
                shownDescription();

            }
        });
        findViewById(R.id.left_arrow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if(!isplaying)soundPool.load(context, R.raw.pressdown, 1);
                selectedtask = (selectedtask + maxtasks -1)%maxtasks;
                shownDescription();

            }
        });

        //add our home screen with the current monster
        final FrameLayout homelayout = (FrameLayout) findViewById(R.id.placeholder);
        LayoutInflater homeinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final View monsterview = homeinflater.inflate(R.layout.home_screen, (ViewGroup)null);
        Fade mFade = new Fade(Fade.IN);
        TransitionManager.beginDelayedTransition(homelayout, mFade);

        homelayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                homelayout.addView(monsterview,0);

                ImageView imageView = monsterview.findViewById(R.id.monster_icon);
                imageView.setBackgroundResource(R.drawable.egg_idle);
                AnimationDrawable monsteranimator = (AnimationDrawable) imageView.getBackground();
                monsteranimator.start();
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
                        int currentarrayid = db.journeyDao().getMonster().get(0).getArrayid();
                        selectedIcon(currentarrayid);
                    }
                });
                homelayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.picker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTask();
            }
        });

        connectionsClient = Nearby.getConnectionsClient(this);

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

    private void shownDescription(){
        int tasktitle = R.string.missing_title;
        int tasktext = R.string.missing_description;
        switch(selectedtask){
            case 0:
                tasktitle = R.string.BattleFriend;
                tasktext = R.string.BattleFriendInfo;
                break;
            case 1:
                tasktitle = R.string.BreedFriend;
                tasktext = R.string.BreedingFriendInfo;
                break;
        }

        title.setText(getText(tasktitle));
        description.setText(getText(tasktext));
    }

    /**
     * send out communication for specified task
     */
    private void startTask(){
        //check if we have permission to use connections on the phone
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
            }
        }
        if(currentmonster == null){
            return;
        }
        isPlayer1 = false;
        switch(selectedtask){
            case 0: //Battle
                Toast.makeText(this,"Connecting", Toast.LENGTH_SHORT).show();
                battlePlayer();
                break;
            case 1:
                int arrayid = currentmonster.getArrayid();
                TypedArray array = getApplicationContext().getResources().obtainTypedArray(arrayid);
                int[] monsterresources = getApplicationContext().getResources().getIntArray(arrayid);
                int stage = monsterresources[0];
                array.recycle();
                if(stage >= 3){
                    Toast.makeText(this,"Connecting", Toast.LENGTH_SHORT).show();
                    breedFriend();
                }else{
                    Toast.makeText(this,"Evolve your monster to the adult stage first!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /**
     * retire our current monster and hatch the new egg
     * @param eggid the new id of the egg
     */
    private void obtainedNewEgg(int eggid){
        //TODO breed/egg animation
        AsyncTask.execute(new Runnable()
        {
            @Override
            public void run()
            {
                AppDatabase db = AppDatabase.buildDatabase(getApplicationContext());
                // run your queries here!
                Journey temp = db.journeyDao().getJourney().get(0);
                temp.setEventsteps(100);
                temp.setEventtype(0);
                temp.setFirsttime(false);
                temp.setEventreached(false);
                db.journeyDao().update(temp);

                //retire the current monster and
                Monster tempmonster = db.journeyDao().getMonster().get(0);
                History temphistory = new History(tempmonster.getGeneration(), tempmonster.getArrayid(), tempmonster.getName());
                db.journeyDao().insertHistory(temphistory);
                //tempmonster.setArrayid(selectedid);
                tempmonster.newEgg(eggid);

                db.journeyDao().updateMonster(tempmonster);
            }
        });

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        //where right side is current view
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
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
        imageView.setBackgroundResource(resource);

        AnimationDrawable monsteranimator = (AnimationDrawable) imageView.getBackground();
        monsteranimator.start();

    }

    /**
     * battle nearby player, both devices will advertise and detect until a connection is established
     */
    private void battlePlayer(){
        currentsearchid = "BattleID";
        trainingtapcount = 0;
        startAdvertising();
        startDiscovery();
    }

    private void breedFriend(){
        currentsearchid = "BreedID";
        startAdvertising();
        startDiscovery();
    }

    /**
     * google nearby connections example code, used for advertising a connection
     */
    private void startAdvertising() {
        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build();
        connectionsClient
                .startAdvertising(
                        currentsearchid, getPackageName(), connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            // We're advertising!Payload.fromBytes(choice.name().getBytes(UTF_8))
                            ByteBuffer b = ByteBuffer.allocate(4);
                            //b.order(ByteOrder.BIG_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.
                            b.putInt(currentmonster.getArrayid());
                            connectionsClient.sendPayload(currentsearchid, Payload.fromBytes(b.array()) );
                            isPlayer1 = true;
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            Toast.makeText(this,"Unable to connect", Toast.LENGTH_SHORT).show();
                            // We were unable to start advertising.
                        });
    }

    /**
     * discovery listener
     */
    private void startDiscovery() {
        DiscoveryOptions discoveryOptions =
                new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build();
        connectionsClient
                .startDiscovery(getPackageName(), endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            ByteBuffer b = ByteBuffer.allocate(4);
                            //b.order(ByteOrder.BIG_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.
                            b.putInt(currentmonster.getArrayid());
                            // We're discovering!
                            connectionsClient.sendPayload(currentsearchid, Payload.fromBytes(b.array()) );
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            Toast.makeText(this,"Unable to connect", Toast.LENGTH_SHORT).show();
                            // We're unable to start discovering.
                        });
    }

    /**
     * callback for the discovery
     */
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    if(endpointId.equals(currentsearchid)){
                        // An endpoint of the same type was found. We request a connection to it.
                        connectionsClient
                                .requestConnection(currentsearchid, endpointId, connectionLifecycleCallback)
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
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    // A previously discovered endpoint has gone away.
                }
            };

    /**
     * lifecycle for our connection
     */
    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    // Automatically accept the connection on both sides.
                    if(endpointId.equals(currentsearchid)){
                        connectionsClient.acceptConnection(endpointId, payloadCallback);
                    }
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            connectionsClient.stopDiscovery();
                            connectionsClient.stopAdvertising();
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // The connection was rejected by one or both sides.
                            break;
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            // The connection broke before it was able to be accepted.
                            break;
                        default:
                            // Unknown status code
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                }
            };

    // Callbacks for receiving payloads
    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    //our payload that we receive
                    //payload.asBytes();
                    if(payload.asBytes() != null && enemyarrayid == 0){
                        ByteBuffer wrapped = ByteBuffer.wrap(payload.asBytes()); // big-endian by default
                        enemyarrayid = wrapped.getInt();
                        Toast.makeText(getApplicationContext(),String.valueOf(enemyarrayid), Toast.LENGTH_SHORT).show();
                    }
                    else if(enemyarrayid != 0 && currentsearchid.equals("BreedID")){
                        //TODO egg stuff here?
                    }

                    //opponentChoice = GameChoice.valueOf(new String(payload.asBytes(), UTF_8));
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS && enemyarrayid != 0) {
                        Toast.makeText(getApplicationContext(), "Starting selected game", Toast.LENGTH_SHORT).show();

                        if(currentsearchid.equals("BattleID")){
                            //can potentially add trainingtapcount value from enemy
                            int[] enemyfound = getResources().getIntArray(enemyarrayid);
                            int enemyattack = enemyfound[6+enemyfound[1]];
                            int enemychance = enemyfound[7+enemyfound[1]];
                            int enemyhealth = enemyfound[5+enemyfound[1]] - 1;
                            int enemymaxhealth = enemyfound[5+enemyfound[1]] - 1;
                            ArrayList<Integer> tempbattleresults = currentmonster.battle(enemyattack,enemyhealth,enemychance, trainingtapcount, true);
                        }else if(currentsearchid.equals("BreedID")){
                            int newegg = currentmonster.breed(enemyarrayid);
                            ByteBuffer b = ByteBuffer.allocate(4);
                            //b.order(ByteOrder.BIG_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.
                            b.putInt(newegg);
                            connectionsClient.sendPayload(currentsearchid, Payload.fromBytes(b.array()) );
                        }

                        //finishRound();
                    }
                }
            };

}
