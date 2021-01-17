package com.application.monsterjourney;

import android.content.Context;
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
    @PrimaryKey(autoGenerate=true)
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

    //mistakes made, by over training or playing minigames while hungry
    @ColumnInfo(name = "mistakes")
    private int mistakes;

    //times fed while hearts are already full
    @ColumnInfo(name = "gluttony")
    private int gluttony;

    //where max hunger is 8, each increment is half a heart
    @ColumnInfo(name = "hunger")
    private int hunger;

    //where max diligence is 8, can increase with training, lowers over time by walking
    @ColumnInfo(name = "diligence")
    private int diligence;

    //steps needed to evolve the monster
    @ColumnInfo(name = "evolve_steps")
    private long evolvesteps;

    @ColumnInfo(name = "monster_name")
    private String name;

    public Monster(int generation){
        this.generation = generation;
        maxhealth = 0;
        hunger = 0;
        diligence = 0;
        gluttony = 0;
        mistakes = 0;
        name = "";
        arrayid = R.array.enigma_egg;
        hatched = false;
    }

    /**
     * called to populate our monster class on initialization of the database
     * @return fresh monster
     */
    public static Monster populateData() {
        return new Monster(0);
    }

    public void hatch(){
        hunger = 4;
        hatched = true;
    }

    public boolean getHatched(){ return hatched;}

    public void setHatched(boolean hatched){ this.hatched =  hatched;}

    public int getGeneration(){return generation;}

    public int getArrayid(){return arrayid;}

    public void setArrayid(int arrayid){this.arrayid = arrayid;}

    public int getMistakes(){return mistakes;}

    public void setMistakes(int mistakes){this.mistakes = mistakes;}

    public int getDiligence(){return diligence;}

    public void setDiligence(int diligence){this.diligence = diligence;}

    public int getHunger(){return hunger;}

    public void setHunger(int hunger){this.hunger = hunger;}

    public int getGluttony(){return gluttony;}

    public void setGluttony(int gluttony){this.gluttony = gluttony;}

    public long getEvolvesteps(){return evolvesteps;}

    public void setEvolvesteps(long newsteps){this.evolvesteps = newsteps;}

    public int getMaxhealth(){return maxhealth;}

    public void setMaxhealth(int maxhealth){this.maxhealth = maxhealth;}

    public int getCurrenthealth(){return currenthealth;}

    public void setCurrenthealth(int currenthealth){this.currenthealth = currenthealth;}

    public int getPower(){return power;}

    public void setPower(int power){this.power = power;}

    public int getChance(){return chance;}

    public void setChance(int chance){this.chance = chance;}

    public void setName(String name){this.name = name;}

    public String getName(){return name;}

    /**
     * create the next egg in our monster after documenting the last one if needed
     * @param arrayid the new arrayid of the monster/egg
     */
    public void newEgg(int arrayid){
        generation = generation++;
        maxhealth = 5;
        hunger = 0;
        gluttony = 0;
        diligence = 0;
        mistakes = 0;
        name = "";
        this.arrayid = arrayid;
        hatched = false;
        evolvesteps = 100;
    }

    /**
     * Update the monster with another monsters values
     * @param monster the class we will copy values over from
     */
    public Monster updateMonster(Monster monster){
        this.generation = monster.getGeneration();
        maxhealth = monster.getMaxhealth();
        hunger = monster.getHunger();
        diligence = monster.getDiligence();
        gluttony = monster.getGluttony();
        mistakes = monster.getMistakes();
        name = monster.getName();
        arrayid = monster.getArrayid();
        hatched = monster.getHatched();
        evolvesteps = monster.getEvolvesteps();

        return this;
    }

    /**
     *
     * @param type the type of food given to the monster
     */
    public void feedMonster(int type){
        if(type == 0 || type == 1 || type == 2){
            int amount = type+1;
            if(hunger < 8){
                hunger = hunger + amount;
                if(hunger > 8){
                    hunger = 8;
                }
            }
            else {
                gluttony++;
            }
        }
        else if(type == 3){
            diligence = diligence+2;
        }
        else if(type == 4){
            evolvesteps = evolvesteps - 1000;
        }

    }

    public void battleLost(){
        if(diligence > 0){
            diligence = diligence - 1;
        }
        else{
            mistakes++;
        }
    }

    /**
     * called when monster evolves, gains new stats and form depending on mistakes and other factors
     */
    public boolean evolve(Context applicationcontext, long evolvediscount){
        if(evolvesteps > 0){
            return false;
        }
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
        } else if(stage >= 3){
            return false;
        }
        else {
            for(int i = 0; i < evolutions; i++){
                @StyleableRes int tempid = 5 + i;
                int tempevolvecheck = array.getResourceId(tempid,R.array.enigma_egg);
                int[] evolutionrequirements = applicationcontext.getResources().getIntArray(tempevolvecheck);
                int evolvenumber = evolutionrequirements[1];
                if(diligence >= evolutionrequirements[evolvenumber+8] && hunger >= evolutionrequirements[evolvenumber+9]
                        && gluttony <= evolutionrequirements[evolvenumber+10] && mistakes <= evolutionrequirements[evolvenumber+11]){
                    criteria[i] = true;
                    break;
                }
                else{
                    criteria[i] = false;
                }
            }
        }

        boolean success = false;

        int temp;
        for(int i = 0; i < evolutions; i++){
            if(criteria[i]){
                @StyleableRes int tempid = 5 + i;
                temp = array.getResourceId(tempid,R.array.enigma_egg);
                //editor.putInt("selectedmonster",temp);
                arrayid = temp;
                success = true;
                if((stage + 1 )== 1){
                    setEvolvesteps(8000 - evolvediscount);
                }
                else if ((stage + 1) ==2){
                    setEvolvesteps(12000-evolvediscount);
                }
/*                editor.putBoolean(String.valueOf(temp),true);
                editor.apply();*/
                break;
            }
        }
        array.recycle();
        return success;

    }

    public void initializebattlestats(Context applicationcontext){
        int[] monsterresources = applicationcontext.getResources().getIntArray(arrayid);
        int evolutions = monsterresources[1];
        chance = monsterresources[evolutions + 7];
        power = monsterresources[evolutions + 6];
        currenthealth = monsterresources[evolutions + 5];
        maxhealth = currenthealth;
    }

    /**
     * Generates the battle event arraylist of how the battle is going
     * result 0 for draw, 1 for player1, 2 for player2
     * @return the result of each step in a battle to be played
     */
    public ArrayList<Integer> battle(int enemyattack, int enemyhealth, int enemychance, int battleboost, boolean online){
        ArrayList<Integer> temp = new ArrayList<>();
        Random ran = new Random();
        int myattack;
        int theirattack;
        int tempplayerhealth = currenthealth;
        if(battleboost > 5){
            battleboost = 5;
        }

        int looplength = 4;
        if(online){
            looplength = 1000;
        }

        for(int i = 0; i < looplength; i++){
            boolean chance1 =  ran.nextInt(100) < (chance + 5*battleboost);
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
                tempplayerhealth = tempplayerhealth - theirattack;
            }
            else {
                temp.add(0);
            }

            if(tempplayerhealth <= 0){
                break;
            }

            if(enemyhealth <= 0){
                break;
            }

        }
        return temp;
    }

    /**
     * find the egg type of the new monster
     * @param othermonstertype the type of the other monster bred with
     * @return egg received from breeding
     */
    public int breed(int othermonstertype, Context applicationcontext){
        TypedArray array = applicationcontext.getResources().obtainTypedArray(othermonstertype);
        int[] monsterresources = applicationcontext.getResources().getIntArray(othermonstertype);
        int evolutions = monsterresources[1];
        @StyleableRes int index = 12+evolutions;
        int newbreedid = array.getResourceId(index, R.array.enigma_egg);
        array.recycle();

        return newbreedid;
    }
}
