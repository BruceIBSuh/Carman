package com.silverback.carman2.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentSnapshot;
import com.ibnco.carman.convertgeocoords.GeoPoint;
import com.ibnco.carman.convertgeocoords.GeoTrans;
import com.silverback.carman2.R;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.FavoriteProviderEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.List;

public class FavoriteGeofenceHelper {

    // Constants
    private static final LoggingHelper log = LoggingHelperFactory.create(FavoriteGeofenceHelper.class);

    // Objects
    private Context context;

    private CarmanDatabase mDB;
    private FavoriteProviderEntity favoriteModel;

    private List<Geofence> mGeofenceList;
    private GeofencingClient mGeofencingClient;
    private PendingIntent mGeofencePendingIntent;
    private OnGeofenceListener mListener;

    // Fields
    //private String geofenceId;
    //private String stnId, stnName;
    //private Location geofenceLocation;
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
        if(mGeofencingClient == null) mGeofencingClient = LocationServices.getGeofencingClient(context);
        if(mDB == null) mDB = CarmanDatabase.getDatabaseInstance(context.getApplicationContext());
        favoriteModel = new FavoriteProviderEntity();
    }

    public void setListener(OnGeofenceListener listener) {
        mListener = listener;
    }

    // Set params required to create geofence objects
    /*
    public void setGeofenceParam(int category, String id, Location location) {
        this.category = category;
        geofenceId = id;
        geofenceLocation = location;
    }
    */

    // Create a geofence, setting the desired location, radius, duration and transition type.
    // Set the stationId or the serviceId(registered time) respectively as the key of Geofence.
    private void createGeofence(String geofenceId, GeoPoint geoPoint){

        if(mGeofenceList == null) mGeofenceList = new ArrayList<>();
        mGeofenceList.add(new Geofence.Builder()
                .setRequestId(geofenceId)
                .setCircularRegion(geoPoint.getY(), geoPoint.getX(), Constants.GEOFENCE_RADIUS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL)//bitwise OR only
                .setLoiteringDelay(Constants.GEOFENCE_LOITERING_TIME)
                //.setNotificationResponsiveness(Constants.GEOFENCE_RESPONSE_TIME)
                .build()
        );
    }

    // Specify the geofences to monitor and to set how related geofence events are triggered.
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        // Tell Location services that GEOFENCE_TRANSITION_ENTER should be triggerd if the device
        // is already inside the geofence despite the triggers are made by entrance and exit.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL);
        builder.addGeofences(mGeofenceList);
        return builder.build();

    }

    // Define an intent(PendingIntent) for geofence transition
    private PendingIntent getGeofencePendingIntent() {

        // Reuse the PendingIntent if we have already have it
        if(mGeofencePendingIntent != null) return mGeofencePendingIntent;

        //Intent intent = new Intent(context, GeofenceTransitionService.class);
        //mGeofencePendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
        //return PendingIntent.getService(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    // Add a provider(gas station / service provider) to Geofence and the Favorite table at the same time.
    // when removing it, not sure how it is safely removed from Geofence, it is deleted from DB, though.
    //public void addGeofenceToFavorite(final String name, final String providerCode, final String addrs) {
    public void addFavoriteGeofence(final DocumentSnapshot snapshot, final String providerId, final int category) {

        GeoPoint geoPoint = null;
        String providerName = null;
        String providerCode = null;
        String address = null;

        switch(category) {
            case Constants.GAS:
                GeoPoint katecPoint = new GeoPoint((double)snapshot.get("katec_x"), (double)snapshot.get("katec_y"));
                geoPoint = GeoTrans.convert(GeoTrans.KATEC, GeoTrans.GEO, katecPoint);

                providerName = snapshot.getString("stn_name");
                providerCode = snapshot.getString("stn_code");
                address = TextUtils.isEmpty(snapshot.getString("new_addrs"))?
                        snapshot.getString("old_addrs"):snapshot.getString("new_addrs");
                break;

            case Constants.SVC:
                if(snapshot.getGeoPoint("geopoint") != null) {
                    double latitude = snapshot.getGeoPoint("geopoint").getLatitude();
                    double longitude = snapshot.getGeoPoint("geopoint").getLongitude();
                    log.i("Google GeoPoint: %s, %s", latitude, longitude);
                    geoPoint = new GeoPoint(longitude, latitude);

                }

                providerName = snapshot.getString("svc_name");
                providerCode = snapshot.getString("svc_code");
                address = snapshot.getString("address");

                break;
        }

        // Create Geofence with the id and the location data.
        createGeofence(providerId, geoPoint);

        //
        favoriteModel.providerName = providerName;
        favoriteModel.category = category;
        favoriteModel.providerId = providerId;
        favoriteModel.providerCode = providerCode;
        favoriteModel.address = address;

        mDB.favoriteModel().insertFavoriteProvider(favoriteModel);

        // Add geofences using addGoefences() which has GeofencingRequest and PendingIntent as parasms.
        // Then, geofences should be saved in the Favorite table as far as they successfully added to
        // geofences. Otherwise, show the error messages using GeofenceStatusCodes
        try {
            mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                    .addOnSuccessListener(aVoid -> {
                        //mDB.favoriteModel().insertFavoriteProvider(favoriteModel);
                        Toast.makeText(context, R.string.geofence_toast_add_favorite, Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
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
                    FavoriteProviderEntity provider = mDB.favoriteModel().findFavoriteProvider(name, id);
                    if(provider != null) {
                        mDB.favoriteModel().deleteProvider(provider);
                        Toast.makeText(context, R.string.toast_remove_favorite, Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> log.i("failed to remove"));

    }

}