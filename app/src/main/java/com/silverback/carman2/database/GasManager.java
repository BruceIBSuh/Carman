package com.silverback.carman2.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class GasManager {

    @PrimaryKey(autoGenerate = true)
    public int _id;

    @ColumnInfo(name = "date_time")
    public long dateTime;

    @ColumnInfo(name = "mileage")
    public int mileage;

    /*
    @ColumnInfo(name = "table_code")
    public int tableCode;
    */

    @ColumnInfo(name = "stn_name")
    public String stnName;

    @ColumnInfo(name = "stn_addrs")
    public String stnAddrs;

    @ColumnInfo(name = "stn_id")
    public String stnId;

    @ColumnInfo(name = "unit_price")
    public int unitPrice;

    @ColumnInfo(name = "total_payment")
    public int totalPayment;

    @ColumnInfo(name = "gas_payment")
    public int gasPayment;

    @ColumnInfo(name = "gas_amount")
    public int gasAmount;

    @ColumnInfo(name = "wash_payment")
    public int washPayment;

    @ColumnInfo(name = "extra_expense")
    public String extraExpense;

    @ColumnInfo(name = "extra_payment")
    public int extraPayment;
}
