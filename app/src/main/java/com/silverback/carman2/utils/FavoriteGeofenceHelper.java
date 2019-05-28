package com.silverback.carman2.utils;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.widget.Toast;

import androidx.lifecycle.LifecycleObserver;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.DataProviderContract;
import com.silverback.carman2.services.GeofenceTransitionService;

import java.util.ArrayList;
import java.util.List;

public class FavoriteGeofenceHelper {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(FavoriteGeofenceHelper.class);

    // Objects
    private Context context;
    private List<Geofence> mGeofenceList;
    private GeofencingClient mGeofencingClient;
    //private Geofence mGeofence;
    private PendingIntent mGeofencePendingIntent;
    private OnGeofenceListener mListener;

    // Fields
    private String geofenceId;
    private String stnId, stnName;
    private Location geofenceLocation;
    private int category; // 1.gas station 2. service center 3. car wash....
    private Uri mNewUri;
    private int rowDeleted;

    // Interface for parent activities
    public interface OnGeofenceListener {
        void notifyAddGeofenceCompleted();
        void notifyAddGeofenceFailed();
    }

    // Constructor for addFavorite()
    public FavoriteGeofenceHelper(Context context) {
        this.context = context;
        mGeofencingClient = LocationServices.getGeofencingClient(context);
    }

    public void setListener(OnGeofenceListener listener) {
        mListener = listener;
    }

    // Set params required to create geofence objects
    public void setGeofenceParam(int category, String id, Location location) {

        this.category = category;
        geofenceId = id;
        geofenceLocation = location;
    }

    // Create Geofence object
    // Set the stationId or serviceId(registered time) as the key of Geofence.
    private void createGeofence(){

        if(mGeofenceList == null) mGeofenceList = new ArrayList<>();

        mGeofenceList.add(new Geofence.Builder()
                .setRequestId(geofenceId)
                .setCircularRegion(geofenceLocation.getLatitude(), geofenceLocation.getLongitude(), Constants.GEOFENCE_RADIUS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL)//bitwise OR only
                .setLoiteringDelay(Constants.GEOFENCE_LOITERING_TIME)
                //.setNotificationResponsiveness(Constants.GEOFENCE_RESPONSE_TIME)
                .build()
        );
    }

    // Specify geofences and initial trigers.
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER|GeofencingRequest.INITIAL_TRIGGER_DWELL);
        builder.addGeofences(mGeofenceList);
        return builder.build();

    }

    // Define an intent(PendingIntent) for geofence transition
    private PendingIntent getGeofencePendingIntent() {

        // Reuse the PendingIntent if we have already have it
        if(mGeofencePendingIntent != null) return mGeofencePendingIntent;

        Intent intent = new Intent(context, GeofenceTransitionService.class);
        mGeofencePendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        log.i("PendingIntent Location Data: %s, %s", geofenceLocation.getLongitude(), geofenceLocation.getLatitude());
        return mGeofencePendingIntent;
        //return PendingIntent.getService(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    // Add a provider(gas station / service provider) to Geofence and the Favorite table at the same time.
    // when removing it, not sure how it is safely removed from Geofence, it is deleted from DB, though.
    public void addFavoriteGeofence(final String name, final String providerCode, final String addrs) {

        // Set Geofencing with a providerId passed to Geofence API as a identifier.
        createGeofence();

        // Add geofences using addGoefences() which has GeofencingRequest and PendingIntent as parasms.
        // Then, geofences should be saved in the Favorite table as far as they successfully added to
        // geofences. Otherwise, show the error messages using GeofenceStatusCodes
        try {
            mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                    .addOnSuccessListener(aVoid -> {
                        // Insert a new provider into the Favorite table
                        ContentValues values = new ContentValues();
                        values.put(DataProviderContract.FAVORITE_PROVIDER_NAME, name);
                        values.put(DataProviderContract.FAVORITE_PROVIDER_CATEGORY, category);
                        values.put(DataProviderContract.FAVORITE_PROVIDER_ID, geofenceId);
                        values.put(DataProviderContract.FAVORITE_PROVIDER_CODE, providerCode);
                        values.put(DataProviderContract.FAVORITE_PROVIDER_ADDRS, addrs);
                        values.put(DataProviderContract.FAVORITE_PROVIDER_LATITUDE, geofenceLocation.getLatitude());
                        values.put(DataProviderContract.FAVORITE_PROVIDER_LONGITUDE, geofenceLocation.getLongitude());

                        mNewUri = context.getContentResolver().insert(DataProviderContract.FAVORITE_TABLE_URI, values);
                        if (mNewUri != null) {
                            log.i("Successfully added favorite");
                            Toast.makeText(context, R.string.geofence_toast_add_favorite, Toast.LENGTH_SHORT).show();
                            //mListener.notifyAddGeofenceCompleted();
                        }
                    })

                    .addOnFailureListener(e -> {
                        log.e("Fail to add favorite: %s", e.getMessage());
                        //mListener.notifyAddGeofenceFailed();
                    });

        } catch(SecurityException e) {
            log.w("SecurityException: %s", e.getMessage());
        }

    }

    // Delete the current station from DB and Geofence. To remove a favorite from Geofence, use
    // removeGeofences() with its requestId which has been already set by setGeofenceParam() and
    // provided when adding it to Favorite.
    public void removeFavoriteGeofence(final String name, final String id) {

        // Create the list which contains requestId's to remove.
        List<String> geofenceId = new ArrayList<>();
        geofenceId.add(id);

        mGeofencingClient.removeGeofences(geofenceId)
                .addOnSuccessListener(aVoid -> {
                    // Remove the corresponding record with the Station ID.
                    final String where = DataProviderContract.FAVORITE_PROVIDER_NAME + " = ? OR "
                            + DataProviderContract.FAVORITE_PROVIDER_ID + " = ?";
                    final String[] args = { name, id };

                    rowDeleted = context.getContentResolver()
                            .delete(DataProviderContract.FAVORITE_TABLE_URI, where, args);

                    if(rowDeleted > 0) {
                        log.i("successfully removed favorite");
                        Toast.makeText(context, R.string.toast_remove_favorite, Toast.LENGTH_SHORT).show();
                    } else log.e("Failed to delete the provider from the db");

                })
                .addOnFailureListener(e -> log.i("failed to remove"));

    }

}