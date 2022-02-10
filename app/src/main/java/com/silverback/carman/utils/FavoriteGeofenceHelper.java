package com.silverback.carman.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman.backgrounds.GeofenceBroadcastReceiver;
import com.silverback.carman.coords.GeoPoint;
import com.silverback.carman.coords.GeoTrans;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.database.FavoriteProviderEntity;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class performs to add or remove a gas station or service center not only to Geofence but
 * also to FavoriteProviderEntity of the local db.
 * GeofencingClient:
 * Geofence.Builder:
 * GeofencingRequest.Builder:
 */
public class FavoriteGeofenceHelper {

    // Constants
    private static final LoggingHelper log = LoggingHelperFactory.create(FavoriteGeofenceHelper.class);

    // Objects
    private final Context context;
    private final FirebaseFirestore firestore;
    private final CarmanDatabase mDB;
    private final FavoriteProviderEntity favoriteModel;
    private final GeofencingClient mGeofencingClient;

    private DocumentReference evalReference;//set or update the "favorite_num" by category.
    private List<Geofence> mGeofenceList;
    private OnGeofenceListener mListener;
    private GeoPoint geoPoint;
    private PendingIntent geofencePendingIntent;

    // Interface for parent activities
    public interface OnGeofenceListener {
        void notifyAddGeofenceCompleted(int placeholder);
        void notifyRemoveGeofenceCompleted(int placeholder);
        void notifyAddGeofenceFailed();
    }

    // Constructor
    public FavoriteGeofenceHelper(Context context) {
        this.context = context;
        mDB = CarmanDatabase.getDatabaseInstance(context.getApplicationContext());
        firestore = FirebaseFirestore.getInstance();
        mGeofencingClient = LocationServices.getGeofencingClient(context);
        favoriteModel = new FavoriteProviderEntity();

    }

    // Attach the listener to ExpenseGasFragment and ExpenseServiceFragment for notifying them
    // of whether to add the provider to Geofence and the DB successfully or not.
    public void setGeofenceListener(OnGeofenceListener listener) {
        mListener = listener;
    }


    // Tell Location services that GEOFENCE_TRANSITION_ENTER should be triggerd if the device
    // is already inside the geofence despite the triggers are made by entrance and exit.
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();

    }

    // PendingIntent is to be handed to GeofencingClient of LocationServices and Geofencingclient,
    // thus, calls the service or broadcast receiver  at a later time.
    // On Android 8.0 (API level 26) and higher, if an app is running in the background while monitoring
    // a geofence, then the device responds to geofencing events every couple of minutes.
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we have already have it
        if(geofencePendingIntent != null) return geofencePendingIntent;
        // Use FLAG_UPDATE_CURRENT so that the same pending intent back when calling addGeofences()
        // and removeGeofences()
        Intent geoIntent = new Intent(context, GeofenceBroadcastReceiver.class);
        geoIntent.setAction(Constants.NOTI_GEOFENCE);
        geofencePendingIntent = PendingIntent.getBroadcast(
                context, 0, geoIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return geofencePendingIntent;
    }

    /*
     * Add a gas station or service center not only to Geofence but also to the FavoriteProviderEntity
     * accroding to the category given as a param.
     * @param snapshot DocumentSnapshot queried from Firestore
     * @param placeHolder the last placeholder retrieved from FavoriteProviderEntity.
     * @param category Service provier b/w Constants.GAS and Constants.SVC.
     */
    @SuppressWarnings("ConstantConditions")
    public void addFavoriteGeofence(DocumentSnapshot snapshot, int placeHolder, int category) {
        final String providerId = snapshot.getId();
        String providerName;
        String providerCode;
        String address;

        switch(category) {
            case Constants.GAS:
                // Get the location data saved as KATEC which is provided by Opinet and convert it
                // to GeoPoint b/c Google map is used to display the station. At a later time when
                // a local map is used, KATEC coords should be applied.
                GeoPoint katecPoint = new GeoPoint((double)snapshot.get("katec_x"), (double)snapshot.get("katec_y"));
                geoPoint = GeoTrans.convert(GeoTrans.KATEC, GeoTrans.GEO, katecPoint);
                providerName = snapshot.getString("stn_name");
                providerCode = snapshot.getString("stn_code");
                address = TextUtils.isEmpty(snapshot.getString("new_addrs"))?
                        snapshot.getString("old_addrs"):snapshot.getString("new_addrs");
                evalReference = firestore.collection("gas_eval").document(providerId);

                log.i("Geofence: %s, %s, %s, %s, %s, %s", providerId,  providerName, providerCode, address, geoPoint.getX(), geoPoint.getY());
                break;

            case Constants.SVC:
                // Service location data should be accumulated on users' own activity and save the
                // data in Firestore, the type of loation data is the geo point, different from the
                // Katec.
                if(snapshot.getGeoPoint("geopoint") != null) {
                    double latitude = snapshot.getGeoPoint("geopoint").getLatitude();
                    double longitude = snapshot.getGeoPoint("geopoint").getLongitude();
                    geoPoint = new GeoPoint(longitude, latitude);
                }

                providerName = snapshot.getString("svc_name");
                providerCode = snapshot.getString("svc_code");
                address = snapshot.getString("address");

                evalReference = firestore.collection("svc_eval").document(providerId);
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + category);
        }

        // Instantiate Genfence.Builder which requires List<Geofence> to add to Geofence.
        if(mGeofenceList == null) mGeofenceList = new ArrayList<>();
        mGeofenceList.add(new Geofence.Builder()
                .setRequestId(providerId)
                .setCircularRegion(geoPoint.getY(), geoPoint.getX(), Constants.GEOFENCE_RADIUS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)//bitwise OR only
                .setLoiteringDelay(Constants.GEOFENCE_LOITERING_TIME)
                .setNotificationResponsiveness(Constants.GEOFENCE_RESPONSE_TIME)
                .build()
        );

        // Set the fields of FavoriteProviderEntity with the snapshot data.
        favoriteModel.providerName = providerName;
        favoriteModel.category = category;
        favoriteModel.providerId = providerId;
        favoriteModel.providerCode = providerCode;
        favoriteModel.address = address;
        favoriteModel.placeHolder = placeHolder;
        favoriteModel.longitude = geoPoint.getX();
        favoriteModel.latitude = geoPoint.getY();


        // Add geofences using addGoefences() which has GeofencingRequest and PendingIntent as parasms.
        // Geofences should be saved in FavoriteProviderEntity as far as they have successfully done
        // Otherwise, show the error messages using GeofenceStatusCodes
        try {
            mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                    .addOnSuccessListener(aVoid -> {
                        // Insert the provider into FavoriteProviderEntity, which is notified to
                        // GeneralFragment by increasing the favorite provider number.
                        mDB.favoriteModel().insertFavoriteProvider(favoriteModel);
                        // Update the favorite_num field of the evaluation collection
                        evalReference.get().addOnCompleteListener(task -> {
                            if(task.isSuccessful()) {
                                DocumentSnapshot doc = task.getResult();
                                if(doc != null && doc.exists()) {
                                    evalReference.update("favorite_num", FieldValue.increment(1));
                                } else {
                                    Map<String, Integer> favorite = new HashMap<>();
                                    favorite.put("favorite_num", 1);
                                    evalReference.set(favorite);
                                }
                            }
                        });

                        // Notify ExpenseGasFragment or ExpenseServiceFragment of the completion of
                        // geofencing.
                        mListener.notifyAddGeofenceCompleted(placeHolder);

                    }).addOnFailureListener(e -> mListener.notifyAddGeofenceFailed());

        } catch(SecurityException e) { e.printStackTrace(); }
    }

    // Delete the current station from DB and Geofence. To remove a favorite from Geofence, use
    // removeGeofences() with its requestId which has been already set by setGeofenceParam() and
    // provided when adding it to Favorite.
    @SuppressWarnings("ConstantConditions")
    public void removeFavoriteGeofence(@Nullable String name, @Nullable String id, int category) {
        // Create the list which contains requestId's to remove.
        List<String> geofenceId = new ArrayList<>();
        geofenceId.add(id);

        switch(category) {
            case Constants.GAS:
                evalReference = firestore.collection("gas_eval").document(id);
                break;
            case Constants.SVC:
                evalReference = firestore.collection("svc_eval").document(id);
                break;
        }

        mGeofencingClient.removeGeofences(geofenceId).addOnSuccessListener(aVoid -> {
            FavoriteProviderEntity provider = mDB.favoriteModel().findFavoriteProvider(name, id);
            if(provider != null) {
                // Delete the provider from FavoriteProviderEntity, which is notified to
                // GeneralFragment by decreasing the favorite provider number.
                int placeholder = provider.placeHolder;
                mDB.favoriteModel().deleteProvider(provider);
                evalReference.get().addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if(doc.exists() && doc.getLong("favorite_num") > 0)
                            evalReference.update("favorite_num", FieldValue.increment(-1));
                    }
                });

                mListener.notifyRemoveGeofenceCompleted(placeholder);
            }
        }).addOnFailureListener(e -> mListener.notifyAddGeofenceFailed());

    }

}