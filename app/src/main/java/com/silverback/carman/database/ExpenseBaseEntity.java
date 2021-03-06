package com.silverback.carman.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class ExpenseBaseEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "rowId")
    public int _id;

    @ColumnInfo(name = "date_time")
    public long dateTime;

    @ColumnInfo(name = "mileage")
    public int mileage;

    @ColumnInfo(name ="category")
    public int category;

    @ColumnInfo(name = "total_expense")
    public int totalExpense;
}
