package com.silverback.carman2.database;

import androidx.room.ColumnInfo;

import com.silverback.carman2.models.Constants;

public class ServiceItemEmbedded {

    @ColumnInfo(name = "name")
    public String itemName;

    @ColumnInfo(name = "price")
    public int itemPrice;

    @ColumnInfo(name = "info")
    public String itemMemo;


}
