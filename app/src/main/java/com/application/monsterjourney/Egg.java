package com.application.monsterjourney;

public class Egg {
    private int eggnumber;
    private boolean eggunlocked;
    /**
     * egg info for our egg selection
     */

    public Egg(int eggnumber, boolean eggunlocked){
        this.eggnumber = eggnumber;
        this.eggunlocked = eggunlocked;
    }

    /**
     *
     * @return the number for the egg
     */
    public int getEggnumber(){
        return eggnumber;
    }

    /**
     *
     * @return if the egg has been unlocked yet
     */
    public boolean getEggUnlocked(){
        return eggunlocked;
    }
}
