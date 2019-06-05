package com.silverback.carman2.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import static androidx.room.OnConflictStrategy.IGNORE;

@Dao
public interface GasManagerDao {

    @Query("SELECT * FROM GasManager ORDER BY _id DESC LIMIT 5")
    List<GasManager> loadRecentGasData();

    @Query("SELECT * FROM GasManager WHERE stn_name = :stnName or stn_id = :stnId")
    GasManager findGasManagerByNameOrId(String stnName, String stnId);

    @Insert(onConflict = IGNORE)
    long insert(GasManager gasManager);

    @Delete
    void deleteGasRecord(GasManager gasManger);

    @Query("DELETE FROM GasManager WHERE stn_name = :stnName OR stn_id = :stnId")
    int deleteGasManager(String stnName, String stnId);

}
