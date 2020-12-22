package com.application.monsterjourney;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "COMPLETEDMAPS")
public class CompletedMaps {
    /**
     * class info for if we have completed the specified map yet, contains steps for each journey type
     * and map array id that contains all the info for that journey
     * for basic types story steps can be 30000,
     * for rarer types story steps can be 50000
     */
    @PrimaryKey(autoGenerate=true)
    public int uid;

    @ColumnInfo(name = "maparray")
    int maparray;

    @ColumnInfo(name = "storysteps")
    long storysteps;

    @ColumnInfo(name = "storycompleted")
    boolean storycompleted;


    public CompletedMaps(int maparray, long storysteps){
        this.maparray = maparray;
        this.storysteps = storysteps;
        storycompleted = false;

    }

    //TODO implement all missing maps, to future proof may need to add update method to db to allow more new monster types to be added in
    public static CompletedMaps[] populateData(){
        return new CompletedMaps[] {
                new CompletedMaps(R.array.enigma_map,30000),
                new CompletedMaps(R.array.dino_map,30000)
        };
    }

    public int getMaparray(){return maparray;}

    public long getStorysteps(){return storysteps;}

    public void setStorycompleted(boolean storycompleted){this.storycompleted = storycompleted;}

    public boolean isStorycompleted(){return storycompleted;}


}
