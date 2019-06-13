package com.silverback.carman2.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class FavoriteProviderEntity {

    @PrimaryKey(autoGenerate = true)
    public int _id;

    @ColumnInfo(name = "favorite_name")
    public String providerName;

    @ColumnInfo(name = "category")
    public int category;

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
}
