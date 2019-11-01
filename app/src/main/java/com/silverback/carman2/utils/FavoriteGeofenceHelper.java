package com.silverback.carman2.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.ibnco.carman.convertgeocoords.GeoPoint;
import com.ibnco.carman.convertgeocoords.GeoTrans;
import com.silverback.carman2.backgrounds.GeofenceTransitionService;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.FavoriteProviderEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoriteGeofenceHelper {

    // Constants
    private static final LoggingHelper log = LoggingHelperFactory.create(FavoriteGeofenceHelper.class);

    // Objects
    private Context context;

    private FirebaseFirestore firestore;
    private CarmanDatabase mDB;
    private FavoriteProviderEntity favoriteModel;

    private DocumentReference evalDocument;//Set or update the "favorite_num" by category.
    private List<Geofence> mGeofenceList;
    private GeofencingClient mGeofencingClient;
    private PendingIntent mGeofencePendingIntent;
    private OnGeofenceListener mListener;

    // Fields
    //private String geofenceId;
    //private String stnId, stnName;
    //private Location geofenceLocation;
    private GeoPoint geoPoint;
    private int category; // 1.gas station 2. service center 3. car wash....
    private Uri mNewUri;
    private int rowDeleted;

    // Interface for parent activities
    public interface OnGeofenceListener {
        void notifyAddGeofenceCompleted();
        void notifyRemoveGeofenceCompleted();
        void notifyAddGeofenceFailed();
    }

    // Constructor
    public FavoriteGeofenceHelper(Context context) {
        this.context = context;
        mGeofencingClient = LocationServices.getGeofencingClient(context);
        mDB = CarmanDatabase.getDatabaseInstance(context.getApplicationContext());
        firestore = FirebaseFirestore.getInstance();
        favoriteModel = new FavoriteProviderEntity();

    }

    // Attach the listener to GasManagerFragment and ServiceManagerFragment for notifying them
    // of whether to add the provider to Geofence and the DB successfully or not.
    public void setGeofenceListener(OnGeofenceListener listener) {
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
    /*
    private void createGeofence(String geofenceId, GeoPoint geoPoint){

        if(mGeofenceList == null) mGeofenceList = new ArrayList<>();
        mGeofenceList.add(new Geofence.Builder()
                .setRequestId(geofenceId)
                .setCircularRegion(geoPoint.getY(), geoPoint.getX(), Constants.GEOFENCE_RADIUS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)//bitwise OR only
                //.setLoiteringDelay(Constants.GEOFENCE_LOITERING_TIME)
                //.setNotificationResponsiveness(Constants.GEOFENCE_RESPONSE_TIME)
                .build()
        );
    }
    */

    // Specify the geofences to monitor and to set how related geofence events are triggered.
    private GeofencingRequest getGeofencingRequest() {

        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        // Tell Location services that GEOFENCE_TRANSITION_ENTER should be triggerd if the device
        // is already inside the geofence despite the triggers are made by entrance and exit.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();

    }

    // PendingIntent is to be handed to GeofencingClient of LocationServices and thus,
    // Geofencingclient calls the explicit service at a later time.
    private PendingIntent getGeofencePendingIntent(String id, String name, int category) {

        // Reuse the PendingIntent if we have already have it
        if(mGeofencePendingIntent != null) return mGeofencePendingIntent;

        log.i("pendingintent extras: %s, %s, %s", id, name, category);
        Intent intent = new Intent(context, GeofenceTransitionService.class);
        // INTENT EXTRAS SEEMS NOT TO WORK WITH GEOFENCE.
        /*
        intent.putExtra("providerId", id);
        intent.putExtra("providerName", name);
        intent.putExtra("category", category);
        */

        mGeofencePendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    // Add a station to Geofence and the Favorite table at the same time.
    // when removing it, not sure how it is safely removed from Geofence, it is deleted from DB, though.
    @SuppressWarnings("ConstantConditions")
    public void addFavoriteGeofence(
            final String userId, final DocumentSnapshot snapshot, final int placeHolder, final int category) {

        final String providerId = snapshot.getId();
        String providerName;
        String providerCode;
        String address;

        switch(category) {

            case Constants.GAS:
                // Get the location data saved as KATEC and convert the data into GEO
                GeoPoint katecPoint = new GeoPoint((double)snapshot.get("katec_x"), (double)snapshot.get("katec_y"));
                geoPoint = GeoTrans.convert(GeoTrans.KATEC, GeoTrans.GEO, katecPoint);

                providerName = snapshot.getString("stn_name");
                providerCode = snapshot.getString("stn_code");
                address = TextUtils.isEmpty(snapshot.getString("new_addrs"))?
                        snapshot.getString("old_addrs"):snapshot.getString("new_addrs");

                evalDocument = firestore.collection("gas_eval").document(providerId);
                break;

            case Constants.SVC:
                if(snapshot.getGeoPoint("geopoint") != null) {
                    double latitude = snapshot.getGeoPoint("geopoint").getLatitude();
                    double longitude = snapshot.getGeoPoint("geopoint").getLongitude();
                    geoPoint = new GeoPoint(longitude, latitude);

                }

                providerName = snapshot.getString("svc_name");
                providerCode = snapshot.getString("svc_code");
                address = snapshot.getString("address");

                evalDocument = firestore.collection("svc_eval").document(providerId);
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + category);
        }

        // Add the station to Geofence.
        //createGeofence(providerId, geoPoint);
        if(mGeofenceList == null) mGeofenceList = new ArrayList<>();
        mGeofenceList.add(new Geofence.Builder()
                .setRequestId(providerId)
                .setCircularRegion(geoPoint.getY(), geoPoint.getX(), Constants.GEOFENCE_RADIUS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)//bitwise OR only
                //.setLoiteringDelay(Constants.GEOFENCE_LOITERING_TIME)
                //.setNotificationResponsiveness(Constants.GEOFENCE_RESPONSE_TIME)
                .build()
        );

        // Set the fields in FavoriteProviderEntity of the local db
        favoriteModel.providerName = providerName;
        favoriteModel.category = category;
        favoriteModel.providerId = providerId;
        favoriteModel.providerCode = providerCode;
        favoriteModel.address = address;
        favoriteModel.placeHolder = placeHolder;
        favoriteModel.longitude = geoPoint.getX();
        favoriteModel.latitude = geoPoint.getY();


        // Add geofences using addGoefences() which has GeofencingRequest and PendingIntent as parasms.
        // Then, geofences should be saved in the Favorite table as far as they successfully added to
        // geofences. Otherwise, show the error messages using GeofenceStatusCodes
        try {
            mGeofencingClient
                    .addGeofences(getGeofencingRequest(), getGeofencePendingIntent(providerId, providerName, category))
                    .addOnSuccessListener(aVoid -> {
                        // Insert the provider into the local db(FavoriteProviderEntity)
                        mDB.favoriteModel().insertFavoriteProvider(favoriteModel);

                        // Upload the geofence to Firestore for purpose of reloading on rebooting.
                        // Seems not working, then refactor requried.
                        Map<String, Object> geofence = new HashMap<>();
                        geofence.put("providerName", providerName);
                        geofence.put("category", category);
                        geofence.put("geopoint", geoPoint);

                        // Upload the Geofence data to "users" for downloading the data when rebooting.
                        firestore.collection("users").document(userId).collection("geofence").document(providerId)
                                .set(geofence, SetOptions.merge())
                                .addOnCompleteListener(task -> {
                                   if(task.isSuccessful()) log.i("Geofence added to Firestore");
                                });

                        // Update the favorite_num field of the evaluation collection
                        evalDocument.get().addOnCompleteListener(task -> {
                            if(task.isSuccessful()) {
                                DocumentSnapshot doc = task.getResult();
                                if(doc != null && doc.exists()) {
                                    evalDocument.update("favorite_num", FieldValue.increment(1));
                                } else {
                                    Map<String, Integer> favorite = new HashMap<>();
                                    favorite.put("favorite_num", 1);
                                    evalDocument.set(favorite);
                                }
                            }
                        });

                        // Notify GasManagerFragment ro ServiceManagerFragment of the completion of
                        // geofencing.
                        mListener.notifyAddGeofenceCompleted();

                    }).addOnFailureListener(e -> {
                        log.e("Fail to add favorite: %s", e.getMessage());
                        mListener.notifyAddGeofenceFailed();
                    });

        } catch(SecurityException e) {
            log.w("SecurityException: %s", e.getMessage());
        }


    }

    // Delete the current station from DB and Geofence. To remove a favorite from Geofence, use
    // removeGeofences() with its requestId which has been already set by setGeofenceParam() and
    // provided when adding it to Favorite.
    @SuppressWarnings("ConstantConditions")
    public void removeFavoriteGeofence(String userId, String name, String id, int category) {

        // Create the list which contains requestId's to remove.
        List<String> geofenceId = new ArrayList<>();
        geofenceId.add(id);

        switch(category) {
            case Constants.GAS:
                evalDocument = firestore.collection("gas_eval").document(id);
                break;
            case Constants.SVC:
                evalDocument = firestore.collection("svc_eval").document(id);
                break;
        }

        mGeofencingClient.removeGeofences(geofenceId).addOnSuccessListener(aVoid -> {

            FavoriteProviderEntity provider = mDB.favoriteModel().findFavoriteProvider(name, id);
            if(provider != null) {
                // Delete the favorite provider out of the local db.
                mDB.favoriteModel().deleteProvider(provider);
                // Remove the geofence out of Firestore with the providerId;
                firestore.collection("users").document(userId)
                        .collection("geofence").document(id).delete()
                        .addOnSuccessListener(bVoid -> log.i("Successfully deleted the geofence"));
                evalDocument.get().addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if(doc.getDouble("favorite_num") > 0)
                            evalDocument.update("favorite_num", FieldValue.increment(-1));
                    }
                });

                mListener.notifyRemoveGeofenceCompleted();
            }
        }).addOnFailureListener(e -> {
            log.i("failed to remove");
            mListener.notifyAddGeofenceFailed();
        });

    }

}