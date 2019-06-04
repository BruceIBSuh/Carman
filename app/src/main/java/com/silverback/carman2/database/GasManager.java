package com.silverback.carman2.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class GasManager {

    @PrimaryKey
    public int _id;

    public long dateTime;
    public int mileage;
    public int tableCode;
    public String stnName;
    public String stnAddrs;
    public String stnId;
    public int gasPrice;
    public int totalPayment;
    public int gasPayment;
    public int gasAmount;
    public int washPayment;
    public String extraExpense;
    public int extraPayment;
}
