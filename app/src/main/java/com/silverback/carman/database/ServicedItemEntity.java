package com.silverback.carman.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(foreignKeys = @ForeignKey(
        entity = ServiceManagerEntity.class, parentColumns = "service_id", childColumns = "svc_id"),
        indices = @Index("svc_id"))

public class ServicedItemEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "item_id")
    public int itemId;

    @ColumnInfo(name = "svc_id")
    public int svcId;

    @ColumnInfo(name = "item_name")
    public String itemName;

    @ColumnInfo(name = "item_price")
    public int itemPrice;

    @ColumnInfo(name = "item_memo")
    public String itemMemo;

}
