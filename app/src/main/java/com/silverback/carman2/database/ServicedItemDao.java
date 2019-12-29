package com.silverback.carman2.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.List;

@Dao
public abstract class ServicedItemDao {

    private static final LoggingHelper log = LoggingHelperFactory.create(ServicedItemDao.class);

    @Query("SELECT item_id FROM ServicedItemEntity WHERE item_name = :name")
    public abstract int queryServicedItemByName(String name);

    @Query("SELECT item_name FROM ServicedItemEntity WHERE svc_id = :id")
    public abstract LiveData<List<String>> queryLatestItems(int id);

}
