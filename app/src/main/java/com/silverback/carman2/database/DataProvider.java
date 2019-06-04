package com.silverback.carman2.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class DataProvider extends ContentProvider {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(DataProvider.class);

    // Constants
    public static final int GAS_TABLE_QUERY = 1;
    public static final int SERVICE_TABLE_QUERY = 2;
    public static final int FAVORITE_TABLE_QUERY = 3;
    public static final int INVALID_URI = -1;



    // Constants for building SQLite tables during initialization
    protected static final String PRIMARY_KEY_TYPE = "INTEGER PRIMARY KEY AUTOINCREMENT";
    protected static final String TEXT_TYPE = "TEXT";
    protected static final String INTEGER_TYPE = "INTEGER"; //means 1-8 bytes including long type.

    // SQL String for creating the GasManagerTable
    protected static final String CREATE_GAS_TABLE_SQL = "CREATE TABLE "
            + DataProviderContract.GAS_TABLE_NAME
            + " ( "
            + DataProviderContract.GAS_ID + " " + PRIMARY_KEY_TYPE + ", "
            + DataProviderContract.TABLE_CODE + " " + INTEGER_TYPE + ", "
            + DataProviderContract.DATE_TIME_COLUMN + " " + INTEGER_TYPE + ", "
            + DataProviderContract.MILEAGE_COLUMN + " " + INTEGER_TYPE + ", "
            + DataProviderContract.GAS_STATION_COLUMN + " " + TEXT_TYPE + ", "
            + DataProviderContract.GAS_STATION_ID_COLUMN + " " + TEXT_TYPE + ", "
            + DataProviderContract.GAS_STATION_ADDRESS_COLUMN + " " + TEXT_TYPE + ", "
            + DataProviderContract.GAS_TOTAL_PAYMENT_COLUMN + " " + INTEGER_TYPE + ", "
            + DataProviderContract.GAS_PRICE_COLUMN + " " + INTEGER_TYPE + ", "
            + DataProviderContract.GAS_PAYMENT_COLUMN + " " + INTEGER_TYPE + ", "
            + DataProviderContract.GAS_AMOUNT_COLUMN + " " + INTEGER_TYPE + ", "
            + DataProviderContract.WASH_PAYMENT_COLUMN + " " + INTEGER_TYPE + ", "
            + DataProviderContract.EXTRA_EXPENSE_COLUMN + " " + TEXT_TYPE + ", "
            + DataProviderContract.EXTRA_PAYMENT_COLUMN + " " + INTEGER_TYPE
            + ");";

    // SQL string for creating the ServiceManagerTable
    protected static final String CREATE_SERVICE_TABLE_SQL = "CREATE TABLE "
            + DataProviderContract.SERVICE_TABLE_NAME
            + " ( "
            + DataProviderContract.SERVICE_ID + " " + PRIMARY_KEY_TYPE + ", "
            + DataProviderContract.TABLE_CODE + " " + INTEGER_TYPE + ", "
            + DataProviderContract.DATE_TIME_COLUMN + " " + INTEGER_TYPE + ", "
            + DataProviderContract.MILEAGE_COLUMN + " " + INTEGER_TYPE + ", "
            + DataProviderContract.SERVICE_PROVIDER_COLUMN + " " + TEXT_TYPE + ", "
            + DataProviderContract.SERVICE_ADDRESS_COLUMN + " " + TEXT_TYPE + ", "
            + DataProviderContract.SERVICE_TOTAL_PRICE_COLUMN + " " + INTEGER_TYPE + ", ";

    // SQL to create the FavoriteProvider table.
    protected static final String CREATE_FAVORITE_TABLE_SQL = "CREATE TABLE "
            + DataProviderContract.FAVORITE_TABLE_NAME
            + " ( "
            + DataProviderContract.FAVORITE_ID + " " + PRIMARY_KEY_TYPE + ", "
            + DataProviderContract.FAVORITE_PROVIDER_CATEGORY + " " + INTEGER_TYPE + ", "
            + DataProviderContract.FAVORITE_PROVIDER_NAME + " " + TEXT_TYPE + ", "
            + DataProviderContract.FAVORITE_PROVIDER_CODE + " " + TEXT_TYPE + ", "
            + DataProviderContract.FAVORITE_PROVIDER_ID + " " + TEXT_TYPE + ", "
            + DataProviderContract.FAVORITE_PROVIDER_ADDRS + " " + TEXT_TYPE + ", "
            + DataProviderContract.FAVORITE_PROVIDER_LATITUDE + " " + INTEGER_TYPE + ", "
            + DataProviderContract.FAVORITE_PROVIDER_LONGITUDE + " " + INTEGER_TYPE
            + ");";



    // Object references
    private CarmanSQLiteOpenHelper mDBHelper;
    private static final UriMatcher sUriMatcher;
    private static final SparseArray<String> sMimeTypes;

    static {

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sMimeTypes = new SparseArray<>();

        sUriMatcher.addURI(
                DataProviderContract.AUTHORITY,
                DataProviderContract.GAS_TABLE_NAME,
                GAS_TABLE_QUERY);

        sUriMatcher.addURI(
                DataProviderContract.AUTHORITY,
                DataProviderContract.SERVICE_TABLE_NAME,
                SERVICE_TABLE_QUERY);

        sUriMatcher.addURI(
                DataProviderContract.AUTHORITY,
                DataProviderContract.FAVORITE_TABLE_NAME,
                FAVORITE_TABLE_QUERY);

        sMimeTypes.put(GAS_TABLE_QUERY,
                DataProviderContract.MIME_TYPE_ROWS + "." + DataProviderContract.GAS_TABLE_NAME);
        sMimeTypes.put(SERVICE_TABLE_QUERY,
                DataProviderContract.MIME_TYPE_ROWS + "." + DataProviderContract.SERVICE_TABLE_NAME);
        sMimeTypes.put(FAVORITE_TABLE_QUERY,
                DataProviderContract.MIME_TYPE_ROWS + "." + DataProviderContract.FAVORITE_TABLE_NAME);

    }

    @Override
    public boolean onCreate() {
        mDBHelper = CarmanSQLiteOpenHelper.getInstance(getContext());
        return true;
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteDatabase db = mDBHelper.getReadableDatabase();

        switch(sUriMatcher.match(uri)) {

            case GAS_TABLE_QUERY:
                // Query against a read-only version of the database
                Cursor gasCursor = db.query(
                        DataProviderContract.GAS_TABLE_NAME,
                        projection,
                        selection, selectionArgs, null, null, null);

                // Sets the ContentResolver to watch this content URI for data changes
                gasCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return gasCursor;

            case SERVICE_TABLE_QUERY:

                Cursor serviceCursor = db.query(
                        DataProviderContract.SERVICE_TABLE_NAME,
                        projection,
                        selection, selectionArgs, null, null, null);

                // Sets the ContentResolver to watch this content URI data be changed.
                serviceCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return serviceCursor;

            case FAVORITE_TABLE_QUERY:

                Cursor favoriteCursor = db.query(
                        DataProviderContract.FAVORITE_TABLE_NAME,
                        projection,
                        selection, selectionArgs, null, null, null);

                favoriteCursor.setNotificationUri(getContext().getContentResolver(), uri);

                return favoriteCursor;

            case INVALID_URI:
                throw new IllegalArgumentException("Query -- Invalid URI:" + uri);
        }

        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return sMimeTypes.get(sUriMatcher.match(uri));
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {

        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        switch(sUriMatcher.match(uri)) {

            case GAS_TABLE_QUERY:
                long gasId = db.insert(
                        DataProviderContract.GAS_TABLE_NAME,
                        null,
                        contentValues
                );

                // If the insert succeeded, notify a change and return the new row's content URI.
                if (gasId != -1) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    return Uri.withAppendedPath(uri, Long.toString(gasId));
                } else {
                    throw new SQLiteException("Insert error:" + uri);
                }

            case SERVICE_TABLE_QUERY:
                long serviceId = db.insert(
                        DataProviderContract.SERVICE_TABLE_NAME,
                        null,
                        contentValues
                );

                // If the insert succeeded, notify a change and return the new row's content URI.
                if (serviceId != -1) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    return Uri.withAppendedPath(uri, Long.toString(serviceId));
                } else {
                    throw new SQLiteException("Insert error:" + uri);
                }

            case FAVORITE_TABLE_QUERY:
                long favoriteId = db.insert(
                        DataProviderContract.FAVORITE_TABLE_NAME,
                        null,
                        contentValues
                );

                if(favoriteId != -1) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    return Uri.withAppendedPath(uri, Long.toString(favoriteId));
                } else {
                    throw new SQLiteException("Insert error: " + uri);
                }
        }

        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selArgs) {

        int rowDeleted;
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        //Log.d(TAG, "Uri: " + sUriMatcher.match(uri));

        switch(sUriMatcher.match(uri)) {

            case GAS_TABLE_QUERY:
                rowDeleted = db.delete(DataProviderContract.GAS_TABLE_NAME, selection, selArgs);

                if(rowDeleted != 0) {
                    if(getContext() != null) getContext().getContentResolver().notifyChange(uri, null);
                    return rowDeleted;
                } else {
                    throw new SQLiteException("Delete Error");
                }

            case SERVICE_TABLE_QUERY:
                rowDeleted = db.delete(DataProviderContract.SERVICE_TABLE_NAME, selection, selArgs);

                if(rowDeleted != 0) {
                    if(getContext() != null) getContext().getContentResolver().notifyChange(uri, null);
                    return rowDeleted;
                } else {
                    throw new SQLiteException("Delete Error");
                }

            case FAVORITE_TABLE_QUERY:
                rowDeleted = db.delete(DataProviderContract.FAVORITE_TABLE_NAME, selection, selArgs);

                if(rowDeleted != 0) {
                    if(getContext() != null) getContext().getContentResolver().notifyChange(uri, null);
                    return rowDeleted;
                } else {
                    throw new SQLiteException("Delete Error");
                }

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);

        }

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public int update(@NonNull Uri uri, ContentValues contentValues, String selection, String[] selectionArgs){

        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        switch(sUriMatcher.match(uri)) {

            case GAS_TABLE_QUERY:
                int rows = db.update(
                        DataProviderContract.GAS_TABLE_NAME,
                        contentValues,
                        selection,
                        selectionArgs);

                // If the update succeeded, notify a change and return the number of updated rows.
                if (rows != 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    return rows;
                } else {
                    throw new SQLiteException("Update error:" + uri);
                }

            case SERVICE_TABLE_QUERY:
                rows = db.update(
                        DataProviderContract.SERVICE_TABLE_NAME,
                        contentValues,
                        selection,
                        selectionArgs);

                // If the update succeeded, notify a change and return the number of updated rows.
                if (rows != 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    return rows;
                } else {
                    throw new SQLiteException("Update error:" + uri);
                }

            case FAVORITE_TABLE_QUERY:
                rows = db.update(
                        DataProviderContract.FAVORITE_TABLE_NAME,
                        contentValues,
                        selection,
                        selectionArgs);

                // If the update succeeded, notify a change and return the number of updated rows.
                if (rows != -1) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    return rows;
                } else {
                    throw new SQLiteException("Update error:" + uri);
                }
        }

        return -1;

    }

    // Closes the SQLite database helper class, to avoid memory leaks. not necessary to close db?
    public void close() {
        mDBHelper.close();
    }
}
