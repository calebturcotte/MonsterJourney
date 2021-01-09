package com.application.monsterjourney;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.concurrent.Executors;

@Database(entities = {Journey.class,Item.class, History.class, Monster.class, UnlockedMonster.class, CompletedMaps.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase db;
    public abstract JourneyDao journeyDao();

    public synchronized static AppDatabase getInstance(Context context) {
        if (db == null) {
            db = buildDatabase(context);
        }
        return db;
    }

    public static AppDatabase buildDatabase(final Context context) {
        // don't do this on a real app!
        //        .allowMainThreadQueries()
        db = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "monster-journey")
                .addCallback(new Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        Executors.newSingleThreadScheduledExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                getInstance(context).journeyDao().insertAll(Journey.populateData());
                                getInstance(context).journeyDao().insertMonster(Monster.populateData());
                                getInstance(context).journeyDao().insertUnlockedMonster(UnlockedMonster.populateData());
                                getInstance(context).journeyDao().insertCompletedMaps(CompletedMaps.populateData());
                            }
                        });
                    }
                })
                .enableMultiInstanceInvalidation()
                .build();
        RoomDatabase.Callback rdc = new RoomDatabase.Callback() {
            public void onCreate (SupportSQLiteDatabase db) {
                // ADD YOUR "Math - Sport - Art - Music" here
            }
            public void onOpen (SupportSQLiteDatabase db) {
                // do something every time database is open
                }
        };
        return db;
    }

    /**
     * example migration tool, used to migrate database versions
     */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE journey "
                    + " ADD COLUMN pub_year INTEGER");
        }
    };



    public static void destroyInstance() {
        db = null;
    }
}
