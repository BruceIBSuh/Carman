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

    @PrimaryKey
    @ColumnInfo(name = "rowid")
    public int _id;

    public String dateTime;
    public String mileage;
    public String tableCode;

    public String serviceCenter;
    public String serviceAddress;
    public String totalPayment;

    public String serviceItem_1;
    public String itemPrice_1;
    public String itemMemo_1;

    public String serviceItem_2;
    public String itemPrice_2;
    public String itemMemo_2;

    public String serviceItem_3;
    public String itemPrice_3;
    public String itemMemo_3;

    public String serviceItem_4;
    public String itemPrice_4;
    public String itemMemo_4;

}

