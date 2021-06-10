package com.silverback.carman.database;

import androidx.room.ColumnInfo;

public class StatData {

    @ColumnInfo(name = "date_time")
    public long dateTime;

    @ColumnInfo(name = "gas_payment")
    public int gasPayment;

    @ColumnInfo(name = "service_payment")
    public int servicePayment;
}
