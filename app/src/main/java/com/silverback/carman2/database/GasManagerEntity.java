package com.silverback.carman2.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(foreignKeys = @ForeignKey(
        entity = ExpenseBaseEntity.class, parentColumns = "_id", childColumns = "basic_id"),
        indices = @Index("basic_id"))

public class GasManagerEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "gas_id")
    public int gasId;

    @ColumnInfo(name = "basic_id")
    public int basicId;

    @ColumnInfo(name = "stn_name")
    public String stnName;

    @ColumnInfo(name = "stn_addrs")
    public String stnAddrs;

    @ColumnInfo(name = "stn_id")
    public String stnId;

    @ColumnInfo(name = "unit_price")
    public int unitPrice;

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
