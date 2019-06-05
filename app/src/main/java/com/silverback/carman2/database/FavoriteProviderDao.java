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

    @Query("SELECT * FROM FavoriteProvider")
    LiveData<List<FavoriteProvider>> loadAllFavoriteProvider();

    @Query("SELECT * FROM FavoriteProvider WHERE favorite_name = :stnName OR favorite_id = :stnId")
    FavoriteProvider findFavoriteProvider(String stnName, String stnId);


    @Query("SELECT favorite_name FROM FavoriteProvider WHERE favorite_name = :stnName OR favorite_id = :stnId")
    String findFavoriteName(String stnName, String stnId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFavoriteProvider(FavoriteProvider favorite);

    @Delete
    void deleteProvider(FavoriteProvider provider);

}
