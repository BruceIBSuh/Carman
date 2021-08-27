package com.silverback.carman.database;

import androidx.lifecycle.LiveData;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Query;

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
            "LEFT JOIN GasManagerEntity ON GasManagerEntity.basic_id = ExpenseBaseEntity._id " +
            "LEFT JOIN ServiceManagerEntity ON ServiceManagerEntity.basic_id = ExpenseBaseEntity._id " +
            "WHERE category = :category1 OR category = :category2 ORDER BY mileage DESC")
    LiveData<List<ExpenseStatements>> loadExpenseByCategory(int category1, int category2);

    @Query("SELECT date_time, total_expense FROM ExpenseBaseEntity WHERE date_time BETWEEN :start AND :end")
    LiveData<List<ExpenseByMonth>> loadTotalExpenseByMonth(long start, long end);

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

}
