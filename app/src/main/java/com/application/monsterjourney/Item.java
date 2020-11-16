package com.application.monsterjourney;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ITEM")
public class Item {
    /**
     * item claimed, where item type is the type of item found
     * item id 1: food that restores 1 heart
     * item id 2: food that restores 1.5 heart
     * item id 3: training pill, fills 1 diligence heart
     * item id 4: special item that reduces steps needed for evolution
     */

    @PrimaryKey(autoGenerate=true)
    public int uid;

    @ColumnInfo(name = "itemtype")
    int itemtype;

    public Item(int itemtype){
        this.itemtype = itemtype;
    }

    public int getitem(){return itemtype;}
}
