package com.silverback.carman2.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ExpenseBaseDao {

    // Retrieve monthly total expense which put gas and service expense in together.
    @Query("SELECT * FROM ExpenseBaseEntity WHERE date_time BETWEEN :start AND :end")
    LiveData<List<ExpenseBaseEntity>> loadExpenseLiveData(long start, long end);

    @Query("SELECT * FROM ExpenseBaseEntity ORDER BY date_time DESC")
    LiveData<ExpenseBaseEntity> loadAllExpenses();

    @Query("SELECT * FROM ExpenseBaseEntity WHERE category = :category ORDER BY date_time DESC")
    LiveData<List<ExpenseBaseEntity>> loadExpsneByCategory(int category);

}
