package com.application.monsterjourney;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "JOURNEY")
public class Journey {
    /**
     * our database entry for pedometer related info
     * steps traveled
     * steps until event
     * if event has been reached and so on
     */
    @PrimaryKey
    public int uid;

    @ColumnInfo(name = "total_steps", defaultValue = "0")
    public long totalsteps;

    @ColumnInfo(name = "event_steps", defaultValue = "0")
    public long eventsteps;

    @ColumnInfo(name = "story_steps", defaultValue = "0")
    public long storysteps;

    @ColumnInfo(name = "story_type")
    public int storytype;

    @ColumnInfo(name = "first_time", defaultValue = "0")
    public boolean firsttime;

    @ColumnInfo(name = "event_reached", defaultValue = "0")
    public boolean eventreached;

    @ColumnInfo(name = "event_type", defaultValue = "0")
    public int eventtype;

    @ColumnInfo(name = "matching")
    public boolean matching;

    @ColumnInfo(name = "matchmaker_steps")
    public long matchmakersteps;

    @ColumnInfo(name = "matchmaker_reached")
    public boolean matchmakerreached;

    @ColumnInfo(name ="battling")
    public boolean isbattling;

    @ColumnInfo(name = "enemyarrayid")
    public int enemyarrayid;

    @ColumnInfo(name = "enemyhealth")
    public int enemyhealth;


    public Journey(){
        totalsteps = 0;
        eventreached = true;
        firsttime = true;
        storysteps = 0;
        //set event steps to 1 so it doesn't trigger an event call the first time loaded in main activity
        eventsteps = 1;
        eventtype = 0;
        matching = false;
        matchmakersteps = 1000;
        matchmakerreached = false;
        storytype = R.array.dino_map;
        storysteps = 30000;
        isbattling = false;
    }
    public static Journey[] populateData() {
        return new Journey[] {
                new Journey()
        };
    }

    /**
     *
     * @param stepsadded the steps added by a source such as minigame
     */
    public void addStepstoJourney(long stepsadded){
        eventsteps -= stepsadded;
        if(eventsteps <= 0){
            eventsteps = 0;
            eventreached = true;
        }
        matchmakersteps -= stepsadded;
        if(matchmakersteps <= 0){
            matchmakersteps = 0;
            matchmakerreached = true;
        }
        storysteps -= stepsadded;
        totalsteps += stepsadded;
    }

    public long getTotalsteps(){
        return totalsteps;
    }

    public void setTotalsteps(long newsteps){
        totalsteps = newsteps;
    }

    public long getEventsteps(){
        return eventsteps;
    }

    public void setEventsteps(long newsteps){
        eventsteps = newsteps;
    }

    public boolean isEventreached(){return eventreached;}

    public void setEventreached(boolean reached){
        eventreached = reached;
    }

    public boolean isFirsttime(){
        return firsttime;
    }

    public void setFirsttime(boolean value){
        firsttime = value;
    }

    public void setEventtype(int type){
        eventtype = type;
    }

    public int getEventtype(){
        return eventtype;
    }

    public boolean isMatching(){return matching;}

    public void setMatching(boolean ismatching){matching = ismatching;}

    public long getMatchmakersteps(){return matchmakersteps;}

    public void setMatchmakersteps(long steps){matchmakersteps = steps;}

    public long getStorysteps(){return storysteps;}

    public void setStorysteps(long storysteps){this.storysteps = storysteps;}

    public int getStorytype(){return storytype;}

    public void setStorytype(int storytype){this.storytype = storytype;}

    public void setIsbattling(boolean isbattling){this.isbattling = isbattling;}

    public boolean getIsbattling(){return isbattling;}

    public void setEnemyarrayid(int enemyarrayid){this.enemyarrayid = enemyarrayid;}

    public int getEnemyarrayid(){return enemyarrayid;}

    public void setEnemyhealth(int enemyhealth){this.enemyhealth = enemyhealth;}

    public int getEnemyhealth(){return enemyhealth;}

}

