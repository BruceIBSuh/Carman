package com.silverback.carman.database;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverter;
import androidx.room.Update;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.silverback.carman.threads.StationFavRunnable;

import java.lang.reflect.Type;
import java.util.List;

@Dao
public interface FavoriteProviderDao {
    // Query all the favorite providers in the entity.
    @Query("SELECT * FROM FavoriteProviderEntity")
    List<FavoriteProviderEntity> loadAllFavoriteProvider();

    @Query("SELECT COUNT(_id) FROM FavoriteProviderEntity WHERE category = :category")
    int countFavoriteNumber(int category);

    @Query("SELECT * FROM FavoriteProviderEntity WHERE category = :category AND placeholder = 0")
    LiveData<FirstSetFavorite> getFirstFav(int category);

    @Query("SELECT * FROM FavoriteProviderEntity WHERE category = :category ORDER BY placeholder ASC")
    LiveData<List<FavoriteProviderEntity>> queryFavoriteProviders(int category);

    @Query("SELECT OS_NM, UNI_ID, category FROM FavoriteProviderEntity WHERE placeholder = 0")
    LiveData<List<FirstSetFavorite>> queryFirstFav();

    @Query("SELECT * FROM FavoriteProviderEntity WHERE OS_NM = :stnName OR UNI_ID = :stnId")
    FavoriteProviderEntity findFavoriteProvider(String stnName, String stnId);

    @Query("SELECT OS_NM FROM FavoriteProviderEntity " +
            "WHERE OS_NM = :stnName AND UNI_ID = :stnId AND category = :category")
    LiveData<String> findFavoriteGasName(String stnName, String stnId, int category);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFavoriteProvider(FavoriteProviderEntity favorite);

    @Update
    void updatePlaceHolder(List<FavoriteProviderEntity> list);

    @Delete
    void deleteProvider(FavoriteProviderEntity provider);
    /*
    @Query("SELECT * FROM FavoriteProviderEntity")
    List<FavoriteProviderEntity> loadAllFavoriteProvider();
    // The total number of the favorite stations and service centers
    @Query("SELECT COUNT(_id) FROM FavoriteProviderEntity WHERE category = :category")
    int countFavoriteNumber(int category);

    @Query("SELECT favorite_id FROM FavoriteProviderEntity WHERE category = :category AND placeholder = 0")
    LiveData<String> getFirstFavorite(int category);

    // Query the favorite list with GAS or SERVICE being sorted.
    @Query("SELECT * FROM FavoriteProviderEntity WHERE category = :category ORDER BY placeholder ASC")
    LiveData<List<FavoriteProviderEntity>> queryFavoriteProviders(int category);

    // Query the favorite provider set in the first placeholder of SettingFavorGasFragment and
    // SettingFavorSvcFragment respectively.
    @Query("SELECT favorite_name, favorite_id, category FROM FavoriteProviderEntity WHERE placeholder = 0")
    LiveData<List<FirstSetFavorite>> queryFirstSetFavorite();

    @Query("SELECT * FROM FavoriteProviderEntity WHERE favorite_name = :stnName OR favorite_id = :stnId")
    FavoriteProviderEntity findFavoriteProvider(String stnName, String stnId);

    @Query("SELECT favorite_name FROM FavoriteProviderEntity " +
            "WHERE favorite_name = :stnName AND favorite_id = :stnId AND category = :category")
    LiveData<String> findFavoriteGasName(String stnName, String stnId, int category);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFavoriteProvider(FavoriteProviderEntity favorite);

    @Update
    void updatePlaceHolder(List<FavoriteProviderEntity> list);

    @Delete
    void deleteProvider(FavoriteProviderEntity provider);
    */
    // class of the result subset which indicates what is the first set station and service in
    // SettingPrefFragment
    class FirstSetFavorite {
        @ColumnInfo(name = "OS_NM")
        public String favoriteName;
        @ColumnInfo(name = "category")
        public int category;
        @ColumnInfo(name = "UNI_ID")
        public String providerId;
        @ColumnInfo(name="OIL_PRICE")
        public String oilPrices;

        @TypeConverter
        public String convPriceListToJson(@NonNull List<StationFavRunnable.OilPrice> oilPriceList) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<StationFavRunnable.OilPrice>>(){}.getType();
            return gson.toJson(oilPriceList, type);
        }

        @TypeConverter
        public List<StationFavRunnable.OilPrice> convJsonToPriceList(String json) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<StationFavRunnable.OilPrice>>(){}.getType();
            return gson.fromJson(json, type);
        }
    }
}
