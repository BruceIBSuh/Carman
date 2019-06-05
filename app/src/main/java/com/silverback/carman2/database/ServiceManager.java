package com.silverback.carman2.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Fts4;
import androidx.room.PrimaryKey;

// Use the class name as the db table name by default. Otherwise, use @Entity(tableName = "xxxxx")
// Likewise, field names are used as column names by default and use @ColumnInfo(name = "xxxx")
// to change names.
@Entity
public class ServiceManager {

    @PrimaryKey(autoGenerate = true)
    public int _id;

    @ColumnInfo(name = "date_time")
    public long dateTime;

    @ColumnInfo(name = "mileage")
    public int mileage;

    /*
    @ColumnInfo(name = "table_code")
    public String tableCode;
    */

    @ColumnInfo(name = "service_name")
    public String serviceCenter;

    @ColumnInfo(name = "service_addrs")
    public String serviceAddrs;

    @ColumnInfo(name = "total_payment")
    public int totalPayment;

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

}

