package com.silverback.carman2.database;

import androidx.room.ColumnInfo;

public class ServiceItemEmbedded {

    @ColumnInfo(name = "item_name")
    public String itemName;

    @ColumnInfo(name = "item_price")
    public int itemPrice;

    @ColumnInfo(name = "item_memo")
    public String itemMemo;
}
