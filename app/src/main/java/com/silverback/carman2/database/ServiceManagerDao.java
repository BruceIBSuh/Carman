package com.silverback.carman2.database;

import androidx.lifecycle.LiveData;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RoomWarnings;
import androidx.room.Transaction;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.List;

@Dao
public abstract class ServiceManagerDao {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceManagerDao.class);

    @Query("SELECT date_time, mileage, total_expense, service_center FROM ServiceManagerEntity " +
            "INNER JOIN BasicManagerEntity ON ServiceManagerEntity.basic_id = BasicManagerEntity._id " +
            "ORDER BY service_id DESC LIMIT 5")
    public abstract LiveData<List<RecentServiceData>> loadRecentServiceData();


    // Fetch the serviced items to be displayed in RecyclerView
    @Query("SELECT item_name, date_time, mileage FROM ServicedItemEntity " +
            "INNER JOIN ServiceManagerEntity ON ServicedItemEntity.svc_id = ServiceManagerEntity.service_id " +
            "INNER JOIN BasicManagerEntity ON ServiceManagerEntity.basic_id = BasicManagerEntity._id " +
            "WHERE item_name IN (:itemNames) ORDER BY date_time DESC")
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    public abstract LiveData<List<ServicedItemData>> loadServicedItemData(String[] itemNames);

    @Query("SELECT item_name, service_center, date_time, mileage FROM ServicedItemEntity " +
            "INNER JOIN ServiceManagerEntity ON ServicedItemEntity.svc_id = ServiceManagerEntity.service_id " +
            "INNER JOIN BasicManagerEntity ON ServiceManagerEntity.basic_id = BasicManagerEntity._id " +
            "WHERE item_name = :itemName ORDER BY date_time DESC LIMIT 1")
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    public abstract ServicedItemData loadServicedItem(String itemName);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertBasics(BasicManagerEntity basicEntity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertService(ServiceManagerEntity svcEntity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void insertServicedItem(ServicedItemEntity itemEntity);

    @Transaction
    public int insertAll(BasicManagerEntity basicEntity,
                         ServiceManagerEntity svcEntity,
                         List<ServicedItemEntity> itemEntityList) {


        long basicId = insertBasics(basicEntity);
        svcEntity.basicId = (int)basicId;
        final int serviceId = (int)insertService(svcEntity);

        for(int i = 0; i < itemEntityList.size(); i++) {
            itemEntityList.get(i).svcId = serviceId;
            insertServicedItem(itemEntityList.get(i));
            log.i("Checked item eneity: %s", itemEntityList.get(i).itemName);
        }

        return serviceId;

    }

    // Static nested class for returning subsets of columns with the joined tables.
    public static class RecentServiceData {
        @ColumnInfo(name = "date_time")
        public long dateTime;
        @ColumnInfo(name = "mileage")
        public int mileage;
        @ColumnInfo(name = "total_expense")
        public int totalExpense;
        @ColumnInfo(name = "service_center")
        public String svcName;
    }

    public static class ServicedItemData {
        @ColumnInfo(name = "date_time")
        public long dateTime;

        @ColumnInfo(name = "mileage")
        public int mileage;

        @ColumnInfo(name = "service_center")
        public String serviceCenter;

        @ColumnInfo(name = "item_name")
        public String itemName;

        /*
        @ColumnInfo(name = "item_price")
        public String itemPrice;

        @ColumnInfo(name = "item_memo")
        public String itemMemo;
        */
    }

}
