package com.silverback.carman.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {
        AutoDataMakerEntity.class, // no use. AutoDataResourceRunnable and task, neither.
        AutoDataModelEntity.class, // no use
        ExpenseBaseEntity.class,
        GasManagerEntity.class,
        ServiceManagerEntity.class,
        ServicedItemEntity.class,
        FavoriteProviderEntity.class},
        version = 2, exportSchema = false)

// The DB class musb be an abstract class and due to its high cost, it should be created in a
// singleton instance.
public abstract class CarmanDatabase extends RoomDatabase {
    private static CarmanDatabase INSTANCE;
    // Abstract methods that has 0 arguments and returns the class that is annotated w/ @Dao.
    public abstract AutoDataDao autoDataModel();
    public abstract ExpenseBaseDao expenseBaseModel();
    public abstract GasManagerDao gasManagerModel();
    public abstract ServiceManagerDao serviceManagerModel();
    public abstract FavoriteProviderDao favoriteModel();
    public abstract ServicedItemDao servicedItemModel();

    // Constructor as a Singleton type
    public static CarmanDatabase getDatabaseInstance(Context context) {
        if(INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                    context.getApplicationContext(), CarmanDatabase.class, "carman.sqlite")
                    .allowMainThreadQueries()
                    .enableMultiInstanceInvalidation()
                    .fallbackToDestructiveMigration()
                    .build();
        }

        return INSTANCE;
    }

    public static void destroyInstance() {
        INSTANCE = null;
    }
}
