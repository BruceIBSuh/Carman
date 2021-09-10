package com.silverback.carman.database;

import androidx.lifecycle.LiveData;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RoomWarnings;
import androidx.room.Transaction;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

import java.util.List;

@Dao
public abstract class ServiceManagerDao {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceManagerDao.class);

    @Query("SELECT service_id, date_time, mileage, total_expense, service_center, service_addrs FROM ServiceManagerEntity " +
            "INNER JOIN ExpenseBaseEntity ON ServiceManagerEntity.basic_id = ExpenseBaseEntity._id " +
            "ORDER BY service_id DESC LIMIT " + Constants.NUM_RECENT_PAGES)
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    public abstract LiveData<List<RecentServiceData>> loadRecentServiceData();

    // Query the latest service to display in GeneralFragment with LIMIT condition as 1.
    @Query("SELECT service_id, date_time, mileage, total_expense, service_center, service_addrs FROM ServiceManagerEntity  " +
            "INNER JOIN ExpenseBaseEntity ON ServiceManagerEntity.basic_id = ExpenseBaseEntity._id " +
            "ORDER BY service_id DESC LIMIT 1")
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    public abstract LiveData<RecentServiceData> loadLatestSvcData();


    // Query the last service history for each item with item name as keyword in ServicedItemEntity
    @Query("SELECT item_name, service_center, date_time, mileage FROM ServicedItemEntity " +
            "INNER JOIN ServiceManagerEntity ON ServiceManagerEntity.service_id = ServicedItemEntity.svc_id " +
            "INNER JOIN ExpenseBaseEntity ON  ExpenseBaseEntity._id = ServiceManagerEntity.basic_id " +
            "WHERE item_name = :itemName ORDER BY date_time DESC LIMIT 1")
    //@SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    public abstract LiveData<LatestServiceData> loadServiceData(String itemName);


    // Insert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertBasics(ExpenseBaseEntity basicEntity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertService(ServiceManagerEntity svcEntity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void insertServicedItem(ServicedItemEntity itemEntity);

    @Transaction
    public int insertAll(ExpenseBaseEntity basicEntity,
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

    /*
     * Subsets of Columns to return when finished to query.
     */

    // Static nested class for returning subsets of columns with the joined tables.
    public static class RecentServiceData {
        @ColumnInfo(name = "service_id")
        public int svcId;
        @ColumnInfo(name = "date_time")
        public long dateTime;
        @ColumnInfo(name = "mileage")
        public int mileage;
        @ColumnInfo(name = "total_expense")
        public int totalExpense;
        @ColumnInfo(name = "service_center")
        public String svcName;
        //@ColumnInfo(name="service_addrs")
        //public String svcAddrs;
    }

    public static class LatestServiceData {
        @ColumnInfo(name = "date_time")
        public long dateTime;

        @ColumnInfo(name = "mileage")
        public int mileage;

        @ColumnInfo(name = "service_center")
        public String serviceCenter;

        @ColumnInfo(name = "item_name")
        public String jsonItemName;

        /*
        @ColumnInfo(name = "item_price")
        public String itemPrice;

        @ColumnInfo(name = "item_memo")
        public String itemMemo;
        */
    }

}
