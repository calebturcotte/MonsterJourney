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

    //tells if the map is of a basic type or a rare type, 0 for basic, 1 for light/dark, 2 for cosmic
    @ColumnInfo(name = "isbasic")
    int isbasic;

    @ColumnInfo(name = "isstarter")
    boolean isstarter;

    @ColumnInfo(name = "isunlocked")
    boolean isunlocked;


    public CompletedMaps(int maparray, long storysteps, int isbasic, boolean isstarter, boolean isunlocked){
        this.maparray = maparray;
        this.storysteps = storysteps;
        storycompleted = false;
        this.isbasic = isbasic;
        this.isstarter = isstarter;
        this.isunlocked = isunlocked;
    }


    /**
     * when adding a new map, be sure it is in the same index as it is in the xml file
     * @return list of all the map data
     */
    public static CompletedMaps[] populateData(){
        return new CompletedMaps[] {
                new CompletedMaps(R.array.enigma_map,25000, 0, true, true),
                new CompletedMaps(R.array.dino_map,25000, 0, true, true),
                new CompletedMaps(R.array.earth_map,25000, 0, false, false),
                new CompletedMaps(R.array.aqua_map,25000, 0, false, false),
                new CompletedMaps(R.array.fire_map,25000, 0, false, false),
                new CompletedMaps(R.array.machine_map,25000, 0, false, false),
                new CompletedMaps(R.array.dark_map,40000, 1, false, false),
                new CompletedMaps(R.array.light_map,40000, 1, false, false),
                new CompletedMaps(R.array.cosmic_map,50000, 2, false, false)
        };
    }

    public int getMaparray(){return maparray;}

    public long getStorysteps(){return storysteps;}

    public void setStorycompleted(boolean storycompleted){this.storycompleted = storycompleted;}

    public boolean isStorycompleted(){return storycompleted;}

    public void setIsbasic(int isbasic){this.isbasic = isbasic;}

    public int isIsbasic(){return isbasic;}

    public void setIsstarter(boolean isstarter){this.isstarter = isstarter;}

    public boolean isIsstarter(){return isstarter;}

    public void setIsunlocked(boolean isunlocked){this.isunlocked = isunlocked;}

    public boolean isIsunlocked(){return isunlocked;}


}
