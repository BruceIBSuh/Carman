package com.silverback.carman2.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;


@Database(
        entities = {GasManager.class, ServiceManager.class, FavoriteProvider.class},
        version = 1,
        exportSchema = false)

public abstract class CarmanDatabase extends RoomDatabase {

    private static CarmanDatabase INSTANCE;

    public abstract GasManagerDao gasManagerModel();
    public abstract ServiceManagerDao serviceManagerMddel();
    public abstract FavoriteProviderDao favoriteProviderModel();

    // Constructor as Singletom type
    public static CarmanDatabase getInMemoryDatabase(Context context) {
        if(INSTANCE == null) {
            //INSTANCE = Room.inMemoryDatabaseBuilder(context.getApplicationContext(), CarmanDatabase.class)
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), CarmanDatabase.class, "carman.sqlite")
                    .allowMainThreadQueries()
                    .enableMultiInstanceInvalidation()
                    //.fallbackToDestructiveMigration()
                    .build();
        }

        return INSTANCE;
    }

    public static void destroyInstance() {
        INSTANCE = null;
    }
}
