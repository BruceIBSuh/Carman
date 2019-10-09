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

    /*
    @Query("SELECT * FROM FavoriteProviderEntity")
    LiveData<List<FavoriteProviderEntity>> loadAllFavoriteProvider();
    */

    // The total number of the favorite stations and service centers
    @Query("SELECT COUNT(_id) FROM FavoriteProviderEntity WHERE category = :category")
    int countFavoriteNumber(int category);

    // Query ther favorite list with GAS or SERVICE being sorted.
    @Query("SELECT * FROM FavoriteProviderEntity WHERE category = :category ORDER BY placeholder ASC")
    LiveData<List<FavoriteProviderEntity>> queryFavoriteProvider(int category);

    // Retrieve the favorite station with the placeholder set first in SettingFavorGasFragment and
    // SettingFavorSvcFragment.
    @Query("SELECT favorite_name, favorite_id, category FROM FavoriteProviderEntity WHERE placeholder = 0")
    LiveData<List<FirstSetFavorite>> queryFirstSetFavorite();


    @Query("SELECT favorite_name, favorite_addrs FROM FavoriteProviderEntity WHERE category = :category")
    LiveData<List<FavoriteNameAddrs>> findFavoriteNameAddrs(int category);


    @Query("SELECT * FROM FavoriteProviderEntity WHERE favorite_name = :stnName OR favorite_id = :stnId")
    FavoriteProviderEntity findFavoriteProvider(String stnName, String stnId);


    @Query("SELECT favorite_name FROM FavoriteProviderEntity WHERE favorite_name = :svcName AND category = :category")
    LiveData<String> findFavoriteSvcName(String svcName, int category);

    @Query("SELECT favorite_name FROM FavoriteProviderEntity " +
            "WHERE favorite_name = :stnName AND favorite_id = :stnId AND category = :category")
    LiveData<String> findFavoriteGasName(String stnName, String stnId, int category);

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

    // class of the result subset which indicates what is the first set station and service in
    // SettingPreferenceFragment
    class FirstSetFavorite {
        @ColumnInfo(name = "favorite_name")
        public String favoriteName;

        @ColumnInfo(name = "category")
        public int category;

        @ColumnInfo(name = "favorite_id")
        public String providerId;
    }
}
