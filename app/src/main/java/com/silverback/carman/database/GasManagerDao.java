package com.silverback.carman.database;

import androidx.lifecycle.LiveData;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
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
            "INNER JOIN ExpenseBaseEntity ON GasManagerEntity.base_id = ExpenseBaseEntity.rowId " +
            "ORDER BY gas_id DESC LIMIT " + Constants.NUM_RECENT_PAGES)
    public abstract LiveData<List<RecentGasData>> loadRecentGasData();

    @Query("SELECT date_time, mileage, stn_name, gas_payment, gas_amount FROM GasManagerEntity  " +
            "INNER JOIN ExpenseBaseEntity ON GasManagerEntity.base_id = ExpenseBaseEntity.rowId " +
            "ORDER BY gas_id DESC LIMIT 1")
    public abstract LiveData<RecentGasData> loadLatestGasData();


    @Query("SELECT date_time, mileage, stn_name, wash_payment FROM GasManagerEntity " +
            "INNER JOIN ExpenseBaseEntity ON GasManagerEntity.base_id = ExpenseBaseEntity.rowId " +
            "WHERE date_time >= :from AND date_time <= :to")
    public abstract LiveData<List<CarWashData>> loadCarWashData(long from, long to);

    @Query("SELECT wash_payment, extra_payment FROM GasManagerEntity " +
            "INNER JOIN ExpenseBaseEntity ON GasManagerEntity.base_id = ExpenseBaseEntity.rowId " +
            "WHERE date_time >= :from AND date_time <= :to")
    public abstract LiveData<List<MiscExpense>> queryMiscExpense(long from, long to);


    @Query("SELECT * FROM GasManagerEntity WHERE stn_name = :stnName or stn_id = :stnId")
    public abstract GasManagerEntity findGasManagerByNameOrId(String stnName, String stnId);


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public int insertTotalExpense(ExpenseBaseEntity totalExpense) {
        return totalExpense._id;
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertGasExpense(GasManagerEntity gasExpense);

    @Transaction
    public void insertTotalAndGasExpense(ExpenseBaseEntity baseEntity, GasManagerEntity gasEntity){
        gasEntity.baseId = insertTotalExpense(baseEntity);
        insertGasExpense(gasEntity);
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertParent(ExpenseBaseEntity basicEntity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insert(GasManagerEntity gasManagerEntity);

    @Transaction
    public long insertBoth(ExpenseBaseEntity baseEneity, GasManagerEntity gasEntity) {
        gasEntity.baseId = (int)insertParent(baseEneity);
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

    public static class MiscExpense {
        @ColumnInfo(name = "wash_payment")
        public int washPayment;
        @ColumnInfo(name = "extra_payment")
        public int extraPayment;
    }



}
