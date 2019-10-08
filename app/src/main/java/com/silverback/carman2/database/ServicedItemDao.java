package com.silverback.carman2.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

@Dao
public abstract class ServicedItemDao {

    private static final LoggingHelper log = LoggingHelperFactory.create(ServicedItemDao.class);

    @Query("SELECT item_id FROM ServicedItemEntity WHERE item_name = :name")
    public abstract int queryServicedItemByName(String name);

}
