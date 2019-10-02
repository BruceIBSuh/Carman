package com.silverback.carman2.database;

import androidx.lifecycle.LiveData;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Ignore;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FavoriteProviderDao {

    @Query("SELECT * FROM FavoriteProviderEntity")
    LiveData<List<FavoriteProviderEntity>> loadAllFavoriteProvider();

    // Query ther favorite list with GAS or SERVICE being sorted.
    @Query("SELECT * FROM FavoriteProviderEntity WHERE category = :category ORDER BY placeholder ASC")
    LiveData<List<FavoriteProviderEntity>> queryFavoriteProvider(int category);

    @Query("SELECT favorite_name, favorite_addrs FROM FavoriteProviderEntity WHERE category = :category")
    LiveData<List<FavoriteNameAddrs>> findFavoriteNameAddrs(int category);


    // Retrieve the favorite station with the placeholder set first in SettingFavorGasFragment and
    // SettingFavorSvcFragment.
    @Query("SELECT * FROM FavoriteProviderEntity WHERE placeholder = 0")
    LiveData<FavoriteProviderEntity> queryFirstSetFavorite();


    @Query("SELECT * FROM FavoriteProviderEntity WHERE favorite_name = :stnName OR favorite_id = :stnId")
    FavoriteProviderEntity findFavoriteProvider(String stnName, String stnId);

    @Query("SELECT favorite_name FROM FavoriteProviderEntity WHERE favorite_name = :svcName AND category = :category")
    LiveData<String> findFavoriteSvcName(String svcName, int category);


    @Query("SELECT favorite_name FROM FavoriteProviderEntity WHERE favorite_name = :stnName OR favorite_id = :stnId")
    LiveData<String> findFavoriteGasName(String stnName, String stnId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFavoriteProvider(FavoriteProviderEntity favorite);

    @Update
    void updatePlaceHolder(List<FavoriteProviderEntity> list);

    @Delete
    void deleteProvider(FavoriteProviderEntity provider);

    class FavoriteNameAddrs {
        @ColumnInfo(name = "favorite_name")
        public String favoriteName;

        @ColumnInfo(name = "favorite_addrs")
        public String favoriteAddrs;
    }

}
