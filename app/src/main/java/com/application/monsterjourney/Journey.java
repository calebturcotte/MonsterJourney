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

    @ColumnInfo(name = "first_time")
    public boolean firsttime;

    @ColumnInfo(name = "showabout")
    public boolean showabout;

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

    @ColumnInfo(name = "enemymaxhealth")
    public int enemymaxhealth;

    @ColumnInfo(name = "evolvediscount")
    public long evolvediscount;

    @ColumnInfo(name = "bossfight")
    public boolean bossfight;

    @ColumnInfo(name = "score1", defaultValue = "0")
    public int score1;

    @ColumnInfo(name = "score2", defaultValue = "0")
    public int score2;


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
        storytype = R.array.enigma_map;
        storysteps = 25000;
        isbattling = false;
        bossfight = false;
        evolvediscount = 0;
        showabout = false;
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
    public void addStepstoJourney(long stepsadded, boolean hatched){
        if(!eventreached){
            eventsteps -= stepsadded;
            if(eventsteps <= 0){
                eventsteps = 0;
                eventreached = true;
            }
            if(matching){
                matchmakersteps -= stepsadded;
                if(matchmakersteps <= 0){
                    matchmakersteps = 0;
                    matchmakerreached = true;
                }
            }

            if(hatched){
                storysteps -= stepsadded;
            }
            totalsteps += stepsadded;
        }
    }

    /**
     * update our high scores
     * @param score the score earned
     */
    public void updateHighScore(int gametype, int score){
        switch (gametype){
            case 0:
                if(score > score1){
                    score1 = score;
                }
                break;
            case 1:
                if(score > score2){
                    score2 = score;
                }
                break;
        }

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

    public boolean isShowabout(){
        return showabout;
    }

    public void setShowabout(boolean value){
        showabout = value;
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

    public void setEnemymaxhealth(int enemymaxhealth){this.enemymaxhealth = enemymaxhealth;}

    public int getEnemymaxhealth(){return enemymaxhealth;}

    public void setEvolvediscount(long evolvediscount){this.evolvediscount = evolvediscount;}

    public long getEvolveddiscount(){return evolvediscount;}

    public  boolean isBossfight(){return bossfight;}

    public void setBossfight(boolean bossfight){this.bossfight = bossfight;}

    public int getScore1(){return score1;}

    public void setScore1(int score1){this.score1 = score1;}

    public int getScore2(){return score2;}

    public void setScore2(int score2){this.score2 = score2;}
}

