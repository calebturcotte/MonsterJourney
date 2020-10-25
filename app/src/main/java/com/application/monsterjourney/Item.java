package com.application.monsterjourney;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ITEM")
public class Item {

    @PrimaryKey(autoGenerate=true)
    public int uid;

    @ColumnInfo(name = "itemtype")
    int itemtype;

    public Item(int itemtype){
        this.itemtype = itemtype;
    }

    public int getitem(){return itemtype;}
}
