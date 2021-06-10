package com.silverback.carman.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.util.List;

@Dao
public abstract class AutoDataDao {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(AutoDataDao.class);

    /*
     * Refer to one to many relationship of Define Relationship b/w Ojects"
     * Instead of using foreign key, Room introduces @Relation annotation, creating new class
     * to combine the primary key and the foreign key and using @Transacion to make a complex
     * so as to return the class. This is an alternative to using foreign keys.
     */

    @Query("SELECT COUNT(_id) FROM AutoDataMakerEntity")
    public abstract int getAutoDataMakerNum();

    @Query("SELECT COUNT(model_id) FROM AutoDataModelEntity")
    public abstract int getAutoDataModelNum();


    // Query model_name with auto_maker as condtion making AutoDataMakerEntity and AutoDataModelEntity
    // inner joined.
    @Query("SELECT model_name FROM AutoDataModelEntity INNER JOIN AutoDataMakerEntity " +
            "On AutoDataModelEntity.parent_id = AutoDataMakerEntity._id " +
            "WHERE auto_maker = :autoMaker ORDER BY model_id ASC")
    public abstract List<String> queryAutoModels(String autoMaker);


    // DELETE All Data
    @Query("DELETE FROM AutoDataModelEntity")
    public abstract void deleteModelData();

    @Query("DELETE FROM AutoDataMakerEntity")
    public abstract void deleteMakerData();

    // Query all the auto makers
    @Query("SELECT auto_maker FROM AutoDataMakerEntity ORDER BY _id ASC")
    public abstract List<String> getAutoMaker();

    // INSERT
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertAutoMaker(AutoDataMakerEntity entity);


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertModel(AutoDataModelEntity entity);

}
