package com.silverback.carman.database;

import androidx.lifecycle.LiveData;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ExpenseBaseDao {
    // Retrieve monthly total expense used in gas and service as well, which is used in StatGraphFragment.
    @Query("SELECT date_time, total_expense FROM ExpenseBaseEntity " +
            "WHERE (category = :category1 OR category = :category2) " +
            "AND date_time BETWEEN :start AND :end")
    LiveData<List<ExpenseByMonth>> loadMonthlyExpense(int category1, int category2, long start, long end);

    // Retrieve the expense by category, which is used in StatStmtsFragment. To fetch the gas expense,
    // the service category is to be -1 to exclude the service expense. The service expense can be
    // likewise fetched.
    @Query("SELECT date_time, mileage, total_expense, stn_name, service_center FROM ExpenseBaseEntity " +
            "LEFT JOIN GasManagerEntity ON GasManagerEntity.base_id = ExpenseBaseEntity.rowId " +
            "LEFT JOIN ServiceManagerEntity ON ServiceManagerEntity.base_id = ExpenseBaseEntity.rowId " +
            "WHERE category = :category1 OR category = :category2 ORDER BY mileage DESC")
    LiveData<List<ExpenseStatements>> loadExpenseByCategory(int category1, int category2);

    @Query("SELECT date_time, total_expense FROM ExpenseBaseEntity WHERE date_time BETWEEN :start AND :end")
    LiveData<List<ExpenseByMonth>> loadTotalExpenseByMonth(long start, long end);

    // Get the total expense of gas used in this month.
    @Query("SELECT category, total_expense FROM ExpenseBaseEntity WHERE date_time BETWEEN :start AND :end ")
    LiveData<List<ExpenseConfig>> queryExpenseConfig(long start, long end);

    // Subset of columns to return from loadMonthlyExpense, which is used in StatGraphFragment.
    class ExpenseByMonth {
        @ColumnInfo(name = "date_time")
        public long dateTime;

        @ColumnInfo(name = "total_expense")
        public int totalExpense;
    }

    // Subset of columns to return from loaddExpenseByCategory, which is used in StatStmtsFragment
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

    class ExpenseConfig {
        @ColumnInfo(name = "category")
        public int category;
        @ColumnInfo(name = "total_expense")
        public int totalExpense;
    }

}
