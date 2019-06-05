package com.silverback.carman2.database;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ServiceManagerDao {

    @Query("SELECT * FROM ServiceManager WHERE rowid = :id")
    ServiceManager loadServiceManager(int id);

    @Query("SELECT * FROM ServiceManager ORDER BY _id DESC LIMIT 5")
    List<ServiceManager> loadRecentServiceData();

    @Query("SELECT date_time, total_payment FROM ServiceManager WHERE date_time BETWEEN :start AND :end")
    public List<StatData> loadServiceExpense(long start, long end);


}
