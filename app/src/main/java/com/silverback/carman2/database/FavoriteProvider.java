package com.silverback.carman2.database;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity
public class FavoriteProvider {

    @PrimaryKey
    public int _id;

    public String providerName;
    public int category;
    public String providerId;
    public String providerCode;
    public String address;
    public double latitude;
    public double longitude;
}
