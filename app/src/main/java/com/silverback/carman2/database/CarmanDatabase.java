package com.silverback.carman2.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;


@Database(entities = {
        ExpenseBaseEntity.class,
        GasManagerEntity.class,
        ServiceManagerEntity.class,
        ServicedItemEntity.class,
        FavoriteProviderEntity.class},
        version = 1, exportSchema = false)

public abstract class CarmanDatabase extends RoomDatabase {

    private static CarmanDatabase INSTANCE;

    public abstract ExpenseBaseDao expenseBaseModel();
    public abstract GasManagerDao gasManagerModel();
    public abstract ServiceManagerDao serviceManagerModel();
    public abstract FavoriteProviderDao favoriteModel();

    // Constructor as Singleton type
    public static CarmanDatabase getDatabaseInstance(Context context) {
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
