package com.silverback.carman2.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import static androidx.room.OnConflictStrategy.IGNORE;

@Dao
public interface GasManagerDao {

    @Query("SELECT * FROM GasManager")
    List<GasManager> loadAllGasRecord();

    @Query("SELECT * FROM GasManager WHERE stnName = :stnName or stnId = :stnId")
    GasManager loadGasManagerByNameOrId(String stnName, String stnId);

    @Insert(onConflict = IGNORE)
    void insert(GasManager gasManager);

    @Delete
    void deleteGasRecord(GasManager gasManger);

    @Query("DELETE FROM GasManager WHERE stnName = :stnName OR stnId = :stnId")
    int deleteGasManager(String stnName, String stnId);

}
