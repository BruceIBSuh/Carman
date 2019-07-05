package com.silverback.carman2.database;

import androidx.lifecycle.LiveData;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RoomWarnings;
import androidx.room.Transaction;

import java.util.List;

@Dao
public abstract class ServiceManagerDao {

    @Query("SELECT date_time, mileage, total_expense, service_center FROM ServiceManagerEntity " +
            "INNER JOIN BasicManagerEntity ON ServiceManagerEntity.basic_id = BasicManagerEntity._id " +
            "ORDER BY service_id DESC LIMIT 5")
    public abstract LiveData<List<RecentServiceData>> loadRecentServiceData();


    // Fetch the serviced items to be displayed in RecyclerView
    @Query("SELECT DISTINCT item_name, item_price, item_memo, service_center, date_time, mileage  FROM ServiceItemEntity " +
            "INNER JOIN ServiceManagerEntity ON ServiceItemEntity.svc_id = ServiceManagerEntity.service_id " +
            "INNER JOIN BasicManagerEntity ON ServiceManagerEntity.basic_id = BasicManagerEntity._id " +
            "WHERE item_name IN (:itemNames) ORDER BY date_time  DESC")

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    public abstract LiveData<List<ServicedItemData>> loadServicedItemData(String[] itemNames);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertBasics(BasicManagerEntity basicEntity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertService(ServiceManagerEntity svcEntity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void insertServceItem(ServiceItemEntity itemEntity);

    @Transaction
    public int insertAll(BasicManagerEntity basicEntity,
                         ServiceManagerEntity svcEntity,
                         List<ServiceItemEntity> servicedItems) {

        long basicId = insertBasics(basicEntity);
        svcEntity.basicId = (int)basicId;
        long serviceId = insertService(svcEntity);

        for(int i = 0; i < servicedItems.size(); i++) {
            servicedItems.get(i).svcId = (int)serviceId;
            insertServceItem(servicedItems.get(i));
        }

        return (int)serviceId;
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

        @ColumnInfo(name = "item_name")
        public String itemName;

        @ColumnInfo(name = "item_price")
        public String itemPrice;

        @ColumnInfo(name = "item_memo")
        public String itemMemo;

    }

}
