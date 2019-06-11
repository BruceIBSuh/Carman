package com.silverback.carman2.database;

import android.net.Uri;
import android.provider.BaseColumns;

public final class DataProviderContract {
    // private constructor
    private DataProviderContract(){}

    private static final String SCHEME = "content";

    public static final String AUTHORITY = "com.silverback.carman2.models.provider";

    private static final Uri CONTENT_URI = Uri.parse(SCHEME + "://" + AUTHORITY);

    // Implements ContentProvider MIME Types
    public static final String MIME_TYPE_ROWS = "vnd.android.cursor.dir/vnd.com.silverback.carman2";
    //public static final String MIME_TPYE_SINGLE_ROW = "vnd.android.cursor.item/vnd.com.ibnco.carman";

    public static final String GAS_ID = BaseColumns._ID;
    public static final String SERVICE_ID = BaseColumns._ID;
    public static final String FAVORITE_ID = BaseColumns._ID;

    // Code to tell GasTable from ServiceTable when querying select
    public static final int GAS_TABLE_CODE = 1;
    public static final int SERVICE_TABLE_CODE = 2;

    public static final String DATE_TIME_COLUMN  = "dateTime";
    public static final String MILEAGE_COLUMN = "mileage";
    public static final String TABLE_CODE = "tableCode";

    // Defines the Gas Table for storing gas related data
    public static final String GAS_TABLE_NAME = "GasManagerEntity";
    public static final Uri GAS_TABLE_URI = Uri.withAppendedPath(CONTENT_URI, GAS_TABLE_NAME);
    public static final String GAS_STATION_COLUMN = "stnName";
    public static final String GAS_STATION_ADDRESS_COLUMN = "stnAddrs";
    public static final String GAS_STATION_ID_COLUMN = "stnId";
    public static final String GAS_PRICE_COLUMN = "gasPrice";
    public static final String GAS_TOTAL_PAYMENT_COLUMN = "totalPayment";
    public static final String GAS_PAYMENT_COLUMN = "gasPayment";
    public static final String GAS_AMOUNT_COLUMN = "gasAmount";
    public static final String WASH_PAYMENT_COLUMN = "washPayment";
    public static final String EXTRA_EXPENSE_COLUMN = "extraExpense";
    public static final String EXTRA_PAYMENT_COLUMN = "extraPayment";

    // Defines the Service Table for storing maintenance related data
    public static final String SERVICE_TABLE_NAME = "ServiceManagerEntity";
    public static final Uri SERVICE_TABLE_URI = Uri.withAppendedPath(CONTENT_URI, SERVICE_TABLE_NAME);
    public static final String SERVICE_PROVIDER_COLUMN = "serviceCenter";
    public static final String SERVICE_ADDRESS_COLUMN = "serviceAddrs";
    public static final String SERVICE_TOTAL_PRICE_COLUMN = "totalPayment";
    public static final String ITEM_NAME = "serviceItem_";
    public static final String ITEM_PRICE = "itemPrice_";
    public static final String ITEM_MEMO = "itemMemo_";

    // Define the Favorites table
    public static final String FAVORITE_TABLE_NAME = "FavoriteProvider";
    public static final Uri FAVORITE_TABLE_URI = Uri.withAppendedPath(CONTENT_URI, FAVORITE_TABLE_NAME);
    public static final String FAVORITE_PROVIDER_NAME = "providerName";
    public static final String FAVORITE_PROVIDER_CATEGORY = "category";
    public static final String FAVORITE_PROVIDER_ID = "providerId"; //gas station id provided by Opinet
    public static final String FAVORITE_PROVIDER_CODE = "providerCode"; //provider identifier: SKC, GS...
    public static final String FAVORITE_PROVIDER_ADDRS = "address";
    public static final String FAVORITE_PROVIDER_LATITUDE = "latitude";
    public static final String FAVORITE_PROVIDER_LONGITUDE = "longitude";


    // Define DB name and version
    public static final String DATABASE_NAME = "carman.sqlite";
    public static final int DATABASE_VERSION = 1;
}
