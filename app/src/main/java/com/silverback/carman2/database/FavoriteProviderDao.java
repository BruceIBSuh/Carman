package com.silverback.carman2.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FavoriteProviderDao {

    @Query("SELECT * FROM FavoriteProviderEntity")
    LiveData<List<FavoriteProviderEntity>> loadAllFavoriteProvider();

    @Query("SELECT * FROM FavoriteProviderEntity WHERE favorite_name = :stnName OR favorite_id = :stnId")
    FavoriteProviderEntity findFavoriteProvider(String stnName, String stnId);


    @Query("SELECT favorite_name FROM FavoriteProviderEntity WHERE favorite_name = :stnName OR favorite_id = :stnId")
    LiveData<String> findFavoriteName(String stnName, String stnId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFavoriteProvider(FavoriteProviderEntity favorite);

    @Delete
    void deleteProvider(FavoriteProviderEntity provider);

}
