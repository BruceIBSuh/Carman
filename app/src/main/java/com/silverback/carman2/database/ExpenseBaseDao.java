package com.silverback.carman2.database;

import androidx.lifecycle.LiveData;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ExpenseBaseDao {

    // Retrieve monthly total expense which put gas and service expense in together.
    @Query("SELECT * FROM ExpenseBaseEntity WHERE date_time BETWEEN :start AND :end")
    LiveData<List<ExpenseBaseEntity>> loadExpenseLiveData(long start, long end);

    @Query("SELECT * FROM ExpenseBaseEntity ORDER BY date_time DESC")
    LiveData<List<ExpenseBaseEntity>> loadAllExpense();

    @Query("SELECT date_time, mileage, total_expense, stn_name, service_center FROM ExpenseBaseEntity " +
            "LEFT JOIN GasManagerEntity ON GasManagerEntity.basic_id = ExpenseBaseEntity._id " +
            "LEFT JOIN ServiceManagerEntity ON ServiceManagerEntity.basic_id = ExpenseBaseEntity._id " +
            "WHERE category = :category1 OR category = :category2 ORDER BY mileage DESC")
    LiveData<List<ExpenseStatements>> loadExpenseByCategory(int category1, int category2);


    // Subset of columns to return
    class ExpenseStatements {

        @ColumnInfo(name = "date_time")
        public long dateTime;

        @ColumnInfo(name = "mileage")
        public int mileage;

        @ColumnInfo(name="total_expense")
        public int totalExpense;

        @ColumnInfo(name="stn_name")
        public String stnName;

        @ColumnInfo(name="service_center")
        public String serviceCenter;
    }

}
