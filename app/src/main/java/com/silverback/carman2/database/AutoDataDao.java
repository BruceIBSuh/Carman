package com.silverback.carman2.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

@Dao
public abstract class AutoDataDao {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(AutoDataDao.class);


    // DELETE All Data
    @Query("DELETE FROM AutoDataEntity")
    public abstract void deleteAllData();

    // INSERT
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insert(AutoDataEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public int insertAutoData(AutoDataEntity autoDataEntity) {
        return (int)insert(autoDataEntity);
    }

}
