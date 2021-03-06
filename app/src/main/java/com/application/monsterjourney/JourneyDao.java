package com.application.monsterjourney;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface JourneyDao {
    //get a single journey object
    @Query("SELECT * FROM journey LIMIT 1")
    List<Journey> getJourney();

    @Query("SELECT * FROM journey")
    List<Journey> getAll();

    @Query("SELECT * FROM journey WHERE uid IN (:userIds)")
    List<Journey> loadAllByIds(int[] userIds);

    @Query("SELECT * FROM journey WHERE total_steps LIKE :first AND " +
            "event_reached LIKE :last LIMIT 1")
    Journey findByName(String first, String last);

    @Insert
    void insertAll(Journey... journeys);

    @Insert
    void insert(Journey journey);

    @Delete
    void delete(Journey journey);

    @Update
    void update(Journey... journey);

    @Query("SELECT * FROM ITEM")
    List<Item> getItems();

    @Insert
    void insertAllItems(Item... items);

    @Insert
    void insertItem(Item item);

    @Delete
    void delete(Item item);


    @Query("SELECT * FROM HISTORY")
    List<History> getHistory();

    @Insert
    void insertHistory(History history);

    @Update
    void updateHistory(List<History> history);

    @Query("SELECT * FROM MONSTER LIMIT 5")
    List<Monster> getMonster();

    @Insert
    void insertMonster(Monster monster);

    @Update
    void updateMonster(Monster monster);

    @Update
    void updateMonster(List<Monster> monsters);

    @Delete
    void delete(Monster monster);

    @Query("SELECT * FROM UNLOCKEDMONSTER")
    List<UnlockedMonster> getUnlockedMonster();

    //for checking if monsters are unlocked we *should only have to update the value if a monster was unlocked before unlocking it
    @Insert
    void insertUnlockedMonster(UnlockedMonster... unlockedMonsters);

    @Update
    void updateUnlockedMonster(List<UnlockedMonster> unlockedMonsters);


    @Query("SELECT * FROM COMPLETEDMAPS")
    List<CompletedMaps> getCompletedMaps();

    //for checking if monsters are unlocked we *should only have to update the value if a monster was unlocked before unlocking it
    @Insert
    void insertCompletedMaps(CompletedMaps... completedMaps);

    @Update
    void updateCompletedMaps(List<CompletedMaps> completedMaps);


}
