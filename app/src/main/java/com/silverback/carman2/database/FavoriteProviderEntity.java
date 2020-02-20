package com.silverback.carman2.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/*
 * This entity(table) is to have favroite gas stations or service centers by category.
 * Getter and setter are not required as far as fields are set to public. Otherwise, they have to
 * be defined to handle values in the entity.
 */
@Entity
public class FavoriteProviderEntity {

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
}
