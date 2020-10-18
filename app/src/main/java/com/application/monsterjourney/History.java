package com.application.monsterjourney;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "HISTORY")
public class History {
    /**
     * a class that has values of the older monsters we had before
     * generation: the generation of the monster
     * id: the array xml id of the monster
     * name: the name of the monster
     */

    @PrimaryKey
    public int uid;

    @ColumnInfo(name = "generation")
    public int generation;

    @ColumnInfo(name = "array_info")
    public int id;

    @ColumnInfo(name = "name")
    public String name;

    public History(int generation, int id, String name){
        this.generation = generation;
        this.id = id;
        this.name = name;
    }

    public int getGeneration(){return generation;}

    public int getId(){return id;}

    public String getName(){return name;}
}
