package com.silverback.carman.database;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Embedded;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Relation;
import androidx.room.Transaction;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

import java.util.List;

@Dao
public abstract class GasManagerDao {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GasManagerDao.class);

    @Query("SELECT date_time, mileage, stn_name, gas_payment, gas_amount FROM GasManagerEntity  " +
            "INNER JOIN ExpenseBaseEntity ON GasManagerEntity.basic_id = ExpenseBaseEntity._id " +
            "ORDER BY gas_id DESC LIMIT " + Constants.NUM_RECENT_PAGES)
    public abstract LiveData<List<RecentGasData>> loadRecentGasData();

    @Query("SELECT date_time, mileage, stn_name, gas_payment, gas_amount FROM GasManagerEntity  " +
            "INNER JOIN ExpenseBaseEntity ON GasManagerEntity.basic_id = ExpenseBaseEntity._id " +
            "ORDER BY gas_id DESC LIMIT 1")
    public abstract LiveData<RecentGasData> loadLatestGasData();


    @Query("SELECT date_time, mileage, stn_name, wash_payment FROM GasManagerEntity " +
            "INNER JOIN ExpenseBaseEntity ON GasManagerEntity.basic_id = ExpenseBaseEntity._id " +
            "WHERE date_time >= :from AND date_time <= :to")

    public abstract LiveData<List<CarWashData>> loadCarWashData(long from, long to);


    @Query("SELECT * FROM GasManagerEntity WHERE stn_name = :stnName or stn_id = :stnId")
    public abstract GasManagerEntity findGasManagerByNameOrId(String stnName, String stnId);

    @Query("SELECT gas_payment, wash_payment FROM GasManagerEntity " +
            "INNER JOIN ExpenseBaseEntity ON GasManagerEntity.basic_id = ExpenseBaseEntity._id " +
            "WHERE date_time >= :from AND date_time <= :to")

    public abstract LiveData<Integer> loadTotalGasAndWashExpense(long from, long to);


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public int insertTotalExpense(ExpenseBaseEntity totalExpense) {
        return totalExpense._id;
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertGasExpense(GasManagerEntity gasExpense);

    @Transaction
    public void insertTotalAndGasExpense(ExpenseBaseEntity baseEntity, GasManagerEntity gasEntity){
        gasEntity.basicId = insertTotalExpense(baseEntity);
        insertGasExpense(gasEntity);
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertParent(ExpenseBaseEntity basicEntity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insert(GasManagerEntity gasManagerEntity);

    @Transaction
    public long insertBoth(ExpenseBaseEntity basicEntity, GasManagerEntity gasEntity) {
        gasEntity.basicId = (int)insertParent(basicEntity);
        return insert(gasEntity);
    }


    @Delete
    abstract void deleteGasRecord(GasManagerEntity gasManger);
    @Query("DELETE FROM GasManagerEntity WHERE stn_name = :stnName OR stn_id = :stnId")
    abstract int deleteGasManager(String stnName, String stnId);


    // Static nested class for returning subsets of columns with the joined tables.
    public static class RecentGasData {
        @ColumnInfo(name = "date_time")
        public long dateTime;
        @ColumnInfo(name = "mileage")
        public int mileage;
        @ColumnInfo(name = "stn_name")
        public String stnName;
        @ColumnInfo(name = "gas_payment")
        public int gasPayment;
        @ColumnInfo(name = "gas_amount")
        public int gasAmount;
    }

    public static class CarWashData {
        @ColumnInfo(name = "date_time")
        public long dateTime;
        @ColumnInfo(name = "mileage")
        public int mileage;
        @ColumnInfo(name = "stn_name")
        public String stnName;
        @ColumnInfo(name = "wash_payment")
        public int washPayment;

    }



}
