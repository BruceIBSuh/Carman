package com.silverback.carman2.database;

import androidx.room.Dao;
import androidx.room.Query;

@Dao
public interface ServiceManagerDao {

    @Query("SELECT * FROM ServiceManager WHERE rowid = :id")
    ServiceManager loadServiceManager(int id);


}
