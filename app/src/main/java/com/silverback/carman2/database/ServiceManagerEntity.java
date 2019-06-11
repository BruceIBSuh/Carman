package com.silverback.carman2.database;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// Use the class name as the db table name by default. Otherwise, use @Entity(tableName = "xxxxx")
// Likewise, field names are used as column names by default and use @ColumnInfo(name = "xxxx")
// to change names.
@Entity(foreignKeys = @ForeignKey(entity = BasicManagerEntity.class,
        parentColumns = "_id", childColumns = "basic_id"),
        indices = @Index("basic_id"))

public class ServiceManagerEntity {

    @PrimaryKey
    @ColumnInfo(name = "service_id")
    public int _id;

    @ColumnInfo(name = "service_name")
    public String serviceCenter;

    @ColumnInfo(name = "service_addrs")
    public String serviceAddrs;

    /*
    @ColumnInfo(name = "custom_item_1")
    public String itemName_1;

    @ColumnInfo(name = "item_price_1")
    public int itemPrice_1;

    @ColumnInfo(name = "item_memo_1")
    public String itemMemo_1;

    @ColumnInfo(name = "custom_item_2")
    public String itemName_2;

    @ColumnInfo(name = "item_price_2")
    public int itemPrice_2;

    @ColumnInfo(name = "item_memo_2")
    public String itemMemo_2;

    @ColumnInfo(name = "custom_item_3")
    public String item_name_3;

    @ColumnInfo(name = "item_price_3")
    public int itemPrice_3;

    @ColumnInfo(name = "item_memo_3")
    public String itemMemo_3;

    @ColumnInfo(name = "custom_item_4")
    public String item_name_4;

    @ColumnInfo(name = "item_price_4")
    public int itemPrice_4;

    @ColumnInfo(name = "item_memo_4")
    public String itemMemo_4;
    */
    @Embedded(prefix = "1")
    public ServiceItemEmbedded serviceItem1;

    @Embedded(prefix = "2")
    public ServiceItemEmbedded serviceItem2;

    @Embedded(prefix = "3")
    public ServiceItemEmbedded serviceItem3;

    @Embedded(prefix = "4")
    public ServiceItemEmbedded serviceItem4;

    @Embedded(prefix = "5")
    public ServiceItemEmbedded serviceItem5;

    @ColumnInfo(name = "basic_id")
    public int basicId;

}

