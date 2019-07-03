package com.silverback.carman2.database;

import androidx.lifecycle.LiveData;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import static androidx.room.OnConflictStrategy.IGNORE;

@Dao
public abstract class ServiceManagerDao {

    @Query("SELECT * FROM ServiceManagerEntity WHERE service_id = :id")
    public abstract ServiceManagerEntity loadServiceManager(int id);

    @Query("SELECT date_time, mileage, service_name, total_expense  FROM ServiceManagerEntity " +
            "INNER JOIN BasicManagerEntity ON ServiceManagerEntity.basic_id = BasicManagerEntity._id " +
            "ORDER BY service_id DESC LIMIT 5")
    public abstract LiveData<List<RecentServiceData>> loadRecentServiceData();

    /*
    @Query("SELECT date_time, total_payment FROM ServiceManagerEntity WHERE date_time BETWEEN :start AND :end")
    public List<StatData> loadServiceExpense(long start, long end);
    */

    @Query("SELECT * FROM ServiceManagerEntity WHERE " +
            "item1_name = :itemName OR " +
            "item2_name = :itemName OR " +
            "item3_name = :itemName OR " +
            "item4_name = :itemName OR " +
            "item5_name = :itemName " +
            "ORDER BY service_id DESC LIMIT 1")

    public abstract LiveData<List<ServiceItemInfo>> loadServiceItemInfo(String itemName);

    @Insert(onConflict = IGNORE)
    abstract long insertParent(BasicManagerEntity basicEntity);

    @Insert(onConflict = IGNORE)
    abstract long insert(ServiceManagerEntity svcEntity);

    @Transaction
    public int insertBoth(BasicManagerEntity basicEntity, ServiceManagerEntity svcEntity) {
        long basicId = insertParent(basicEntity);
        svcEntity.basicId = (int)basicId;
        long svcId = insert(svcEntity);
        return (int)svcId;
    }

    // Static nested class for returning subsets of columns with the joined tables.
    public static class RecentServiceData {
        @ColumnInfo(name = "date_time")
        public long dateTime;
        @ColumnInfo(name = "mileage")
        public int mileage;
        @ColumnInfo(name = "total_expense")
        public int totalExpense;
        @ColumnInfo(name = "service_name")
        public String svcName;
    }

    public static class ServiceItemInfo {

    }

}
