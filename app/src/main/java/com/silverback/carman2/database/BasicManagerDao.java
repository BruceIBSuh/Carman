package com.silverback.carman2.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BasicManagerDao {

    // Retrieve monthly total expense which put gas and service expense in together.
    @Query("SELECT * FROM BasicManagerEntity WHERE date_time BETWEEN :start AND :end")
    LiveData<List<BasicManagerEntity>> loadExpenseLiveData(long start, long end);

}
