package com.application.monsterjourney;

public class ItemAmount {
    /**
     * storage for our items to show on display
     */
    int type;
    int amount;
    boolean unlocked;

    public ItemAmount(int type, int amount){
        this.type = type;
        this.amount = amount;
        unlocked = amount > 0;
    }

    /**
     *
     * @return the type of the item
     */
    public int getType(){ return type;}

    /**
     *
     * @return the amount of the item the player has
     */
    public int getAmount(){return amount;}

    /**
     * check the inverted for if it is unlocked or not
     * @return if the amount we have of the item is big enough to be unlocked
     */
    public boolean isUnlocked(){return !unlocked;}
}
