package com.application.monsterjourney;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(tableName = "UNLOCKEDMONSTER")
public class UnlockedMonster {
    /**
     * class info for if we have unlocked the specified monster yet
     */
    @PrimaryKey(autoGenerate=true)
    public int uid;

    @ColumnInfo(name = "monsterarrayid")
    int monsterarrayid;

    //where stage 0 is egg, stage 1 is baby, 2 is child, and 3 is adult
    @ColumnInfo(name = "stage")
    int stage;

    @ColumnInfo(name = "unlocked")
    boolean unlocked;

    @ColumnInfo(name = "discovered")
    boolean discovered;

    public UnlockedMonster(int monsterarrayid,int stage, boolean unlocked){
        this.monsterarrayid = monsterarrayid;
        this.unlocked = unlocked;
        discovered = unlocked;
        this.stage = stage;
    }

    //TODO implement all missing monsters, to future proof may need to add update method to db to allow more new monster types to be added in
    public static UnlockedMonster[] populateData(){
        return new UnlockedMonster[] {
                new UnlockedMonster(R.array.basic_egg,0, true),
                new UnlockedMonster(R.array.dino_egg, 0,true),
                new UnlockedMonster(R.array.basic_baby1,1, false),
                new UnlockedMonster(R.array.basic_baby2,1, false),
                new UnlockedMonster(R.array.dino_baby1,1, false),
                new UnlockedMonster(R.array.dino_baby2, 1,false),
                new UnlockedMonster(R.array.dino_child1,2, false),
                new UnlockedMonster(R.array.dino_child2, 2,false),
                new UnlockedMonster(R.array.dino_child3, 2,false),
                new UnlockedMonster(R.array.dino_child4, 2,false),
                new UnlockedMonster(R.array.dino_child5,2, false),
                new UnlockedMonster(R.array.dino_adult1, 3,false),
                new UnlockedMonster(R.array.dino_adult2, 3,false),
                new UnlockedMonster(R.array.dino_adult4, 3,false),
                new UnlockedMonster(R.array.dino_adult5, 3,false),
                new UnlockedMonster(R.array.dino_adult6, 3,false),
                new UnlockedMonster(R.array.dino_adult7, 3,false),
                new UnlockedMonster(R.array.dino_adult8, 3,false)

        };
    }

    public int getMonsterarrayid(){return monsterarrayid;}

    public void setUnlocked(boolean unlocked){this.unlocked = unlocked;}

    public boolean isUnlocked(){return unlocked;}

    public void setDiscovered(boolean discovered){this.discovered = discovered;}

    public boolean isDiscovered(){return discovered;}

    public int getStage(){return stage;}
}
