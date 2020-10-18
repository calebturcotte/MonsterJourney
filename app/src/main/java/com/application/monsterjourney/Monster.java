package com.application.monsterjourney;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import androidx.annotation.StyleableRes;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.Random;

@Entity(tableName = "MONSTER")
public class Monster {
    /**
     * The current monster
     */
    @PrimaryKey
    public int uid;

    @ColumnInfo(name = "hatched", defaultValue = "false")
    private boolean hatched;
    @ColumnInfo(name = "generation", defaultValue = "0")
    private int generation;

    private int maxhealth;
    private int currenthealth;
    private int power;
    private int chance;

    @ColumnInfo(name = "monster_array")
    private int arrayid;

    @ColumnInfo(name = "mistakes")
    private int mistakes;
    @ColumnInfo(name = "hunger")
    private int hunger;

    public Monster(int generation, boolean hatched){
        this.generation = generation;
        maxhealth = 5;
        arrayid = R.array.basic_egg;
    }

    /**
     * called to populate our monster class on initialization of the database
     * @return fresh monster
     */
    public static Monster populateData() {

        return new Monster(0,false);

    }

    public void hatch(){
        hatched = true;
    }

    public boolean getHatched(){ return hatched;}

    public int getGeneration(){return generation;}

    public int getArrayid(){return arrayid;}

    public void setArrayid(int arrayid){this.arrayid = arrayid;}

    public int getMistakes(){return mistakes;}

    public void setMistakes(int mistakes){this.mistakes = mistakes;}

    public int getHunger(){return hunger;}

    public void setHunger(int hunger){this.hunger = hunger;}

    public int getMaxhealth(){return maxhealth;}

    public void setMaxhealth(int maxhealth){this.maxhealth = maxhealth;}

    public int getCurrenthealth(){return currenthealth;}

    public void setCurrenthealth(int currenthealth){this.currenthealth = currenthealth;}

    public int getPower(){return power;}

    public void setPower(int power){this.power = power;}

    public int getChance(){return chance;}

    public void setChance(int chance){this.chance = chance;}

    /**
     * called when monster evolves, gains new stats and form depending on mistakes and other factors
     */
    public void evolve(Context applicationcontext){
        TypedArray array = applicationcontext.getResources().obtainTypedArray(arrayid);
        int[] monsterresources = applicationcontext.getResources().getIntArray(arrayid);
        int evolutions = monsterresources[1];
        int stage = monsterresources[0];

        boolean[] criteria = new boolean[evolutions];

        Random ran = new Random();

        //SharedPreferences.Editor editor = settings.edit();


        //criteria = (stage == 0);
        if(stage == 0){
            criteria[0] = ran.nextFloat() < 0.5f;
            criteria[1] = true;
        }

        int temp;
        for(int i = 0; i < evolutions; i++){
            if(criteria[i]){
                @StyleableRes int tempid = 5 + i;
                temp = array.getResourceId(tempid,R.array.basic_egg);
                //editor.putInt("selectedmonster",temp);
                arrayid = temp;
/*                editor.putBoolean(String.valueOf(temp),true);
                editor.apply();*/
                break;
            }
        }
        array.recycle();


    }

    /**
     * Generates the battle event arraylist of how the battle is going
     * result 0 for draw, 1 for player1, 2 for player2
     * @return the result of each step in a battle to be played
     */
    public ArrayList<Integer> battle(int enemyattack, int enemyhealth, int enemychance){
        ArrayList<Integer> temp = new ArrayList<>();
        Random ran = new Random();
        int myattack;
        int theirattack;
        currenthealth = maxhealth;
        for(int i = 0; i < 4; i++){
            boolean chance1 =  ran.nextInt(100) < chance;
            boolean chance2 = ran.nextInt(100) < enemychance;
            if(chance1){
                myattack = power;
            }else{
                myattack = 0;
            }
            if(chance2){
                theirattack = enemyattack;
            }
            else{
                theirattack = 0;
            }

            if(myattack > theirattack){
                temp.add(1);
                enemyhealth = enemyhealth - myattack;
            }
            else if(theirattack > myattack){
                temp.add(2);
                currenthealth = currenthealth - theirattack;
            }
            else {
                temp.add(0);
            }

            if(currenthealth < 0){
                break;
            }

            if(enemyhealth < 0){
                break;
            }

        }
        return temp;
    }

    /**
     *
     * @param othermonstertype the type of the other monster bred with
     * @return egg received from breeding
     */
    public int breed(int othermonstertype){

        return R.array.basic_egg;
    }
}
