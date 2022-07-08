package com.silverback.carman.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.silverback.carman.threads.StationFavRunnable;

import java.util.List;
import java.util.Map;

/*
 * This entity(table) is to have favroite gas stations or service centers by category.
 * Getter and setter are not required as far as fields are set to public. Otherwise, the methods have
 * to be defined to handle values in the entity.
 */
@Entity
public class FavoriteProviderEntity {
    @PrimaryKey(autoGenerate = true)
    public int _id;
    @ColumnInfo(name = "UNI_ID")
    public String stationId;
    @ColumnInfo(name = "category")
    public int category;
    @ColumnInfo(name = "placeholder")
    public int placeHolder;
    @ColumnInfo(name = "OS_NM")
    public String stationName;
    @ColumnInfo(name = "POLL_DIV_CD")
    public String company;
    @ColumnInfo(name = "VAN_ADR")
    public String addrsOld;
    @ColumnInfo(name = "NEW_ADR")
    public String addrsNew;
    @ColumnInfo(name = "geo_latitude")
    public double latitude;
    @ColumnInfo(name = "geo_longitude")
    public double longitude;
    @ColumnInfo(name = "CAR_WASH_YN")
    public String carWashYN;
    @ColumnInfo(name = "CVS_YN")
    public String cvsYN;
    @ColumnInfo(name = "MAINT_YN")
    public String maintYN;
    @ColumnInfo(name = "OIL_PRICE")
    public String oilPrices;

    /*
    @PrimaryKey(autoGenerate = true)
    public int _id;

    @ColumnInfo(name = "favorite_name")
    public String providerName;

    @ColumnInfo(name = "category")
    public int category;

    @ColumnInfo(name = "placeholder")
    public int placeHolder;

    @ColumnInfo(name = "favorite_id")
    public String providerId;

    @ColumnInfo(name ="favorite_code")
    public String providerCode;

    @ColumnInfo(name = "favorite_addrs")
    public String address;

    @ColumnInfo(name = "geo_latitude")
    public double latitude;

    @ColumnInfo(name = "geo_longitude")
    public double longitude;
     */
}
