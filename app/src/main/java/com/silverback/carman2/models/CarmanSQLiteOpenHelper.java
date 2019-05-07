package com.silverback.carman2.models;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class CarmanSQLiteOpenHelper extends SQLiteOpenHelper {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(CarmanSQLiteOpenHelper.class);

    // Singleton SqliteOpenHelper constructor
    //@SuppressLint("StaticFieldLeak")
    private static CarmanSQLiteOpenHelper sInstance = null;

    // Constructor(private). Accessible by getInstance()
    private CarmanSQLiteOpenHelper(Context context) {
        super(
                context,
                DataProviderContract.DATABASE_NAME,
                null,
                DataProviderContract.DATABASE_VERSION
        );
    }

    // static method to get Singleton SQLiteOpenHelper instance
    public static CarmanSQLiteOpenHelper getInstance(Context context) {
        // Use the <application context> to prevent this from accedentally leaking an reference to
        // Activity or Fragment when the long-running handler/loader/task will hold it, which leads
        // to fail GC.
        if(sInstance == null) {
            sInstance = new CarmanSQLiteOpenHelper(context.getApplicationContext());
        }

        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        //String[] items = context.getResources().getStringArray(R.array.service_check_list);

        // Make up the SQL string which is composed of the basic string defined in DataProvider,
        // then append the string to add as many service item columns(name, price, and memo)
        // as defined in Constants.Service_Item_Columns(currently 5 items)
        StringBuilder strbldr = new StringBuilder(DataProvider.CREATE_SERVICE_TABLE_SQL);

        // SQL for creating service item name, price, and memo columns up to the preset number
        for (int i = 1; i < Constants.SERVICE_ITEM_COLUMNS; i++) {
            String column = DataProviderContract.ITEM_NAME + i + " " + DataProvider.TEXT_TYPE + ", "
                    + DataProviderContract.ITEM_PRICE + i + " " + DataProvider.INTEGER_TYPE + ", "
                    + DataProviderContract.ITEM_MEMO + i + " " + DataProvider.TEXT_TYPE + ", ";
            strbldr.append(column);
        }

        // Separate SQL for the last columns from the previous ones to define the parenthesis.
        String lastColumn = DataProviderContract.ITEM_NAME + Constants.SERVICE_ITEM_COLUMNS + " " + DataProvider.TEXT_TYPE + ", "
                + DataProviderContract.ITEM_PRICE + Constants.SERVICE_ITEM_COLUMNS  + " " + DataProvider.INTEGER_TYPE + ", "
                + DataProviderContract.ITEM_MEMO + Constants.SERVICE_ITEM_COLUMNS + " " + DataProvider.TEXT_TYPE
                + ");";
        strbldr.append(lastColumn);

        //Log.d(TAG, "ServiceManager table sql: " + strbldr);
        db.execSQL(DataProvider.CREATE_GAS_TABLE_SQL); // Create GasTable
        db.execSQL(strbldr.toString());  // Create ServiceTable
        db.execSQL(DataProvider.CREATE_FAVORITE_TABLE_SQL); // Create FavoriteProvider table.
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Log.w(TAG, "onUpdate SQLite from: " + oldVersion + " to " + newVersion);
        // VERSION 2: add requestId column to Favorite table
        /*
        String alterSQL = "ALTER TABLE "
                + DataProviderContract.FAVORITE_TABLE_NAME
                + " ADD COLUMN "
                + DataProviderContract.FAVORITE_REQUEST_ID + " "
                + DataProvider.TEXT_TYPE;
        db.execSQL(alterSQL);
        */
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int version1, int version2) {
        Log.w(CarmanSQLiteOpenHelper.class.getName(),
                "Downgrading database from version " + version1 + " to "
                        + version2 + ", which will destroy all the existing data");
        dropTables(db);
        onCreate(db);
    }

    private void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + DataProviderContract.GAS_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DataProviderContract.SERVICE_TABLE_NAME);
    }
}
