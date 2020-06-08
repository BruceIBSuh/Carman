package com.silverback.carman2.backgrounds;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.FavoriteProviderEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * This class subclasses Worker class to re-register the previously registered geofences. Registered
 * geofences are kept in the com.google.process.location process owned by the com.google.android.gms.
 * However, when the device is rebooted, reinstalled, or the app data is cleard, registered geofences
 * must be re-registered.
 *
 * To do so, WorkManager is appropriate to reset the geofences, which is first required to creates
 * a custom class subclassing Worker class, then define WorkRequest which contains the work class
 * as param in the constructor. To submit the workrequest to the system, WorkManager should be
 * instantiated to put the workrequest in enqueue(workrequest).
 *
 * The time the workmanager is going to be executed depends upon the constraints used in WorkRequest
 * and system optimization as well.
 */

public class GeofenceResetWorker extends Worker {

    private static final LoggingHelper log = LoggingHelperFactory.create(GeofenceResetWorker.class);

    private Context context;
    private List<Geofence> geofenceList;
    private PendingIntent mGeofencePendingIntent;

    public GeofenceResetWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
        geofenceList = new ArrayList<>();
    }

    @NonNull
    @Override
    public Result doWork() {
        resetGeofence();
        return Result.success();
    }

    private void resetGeofence() {
        GeofencingClient geofencingClient = LocationServices.getGeofencingClient(context);
        CarmanDatabase mDB = CarmanDatabase.getDatabaseInstance(context);
        List<FavoriteProviderEntity> favoriteList = mDB.favoriteModel().loadAllFavoriteProvider();

        // Registered geofences have been saved in the FavoriteProviderEntity of the Room database,
        // thus, retrieve the geofences, put them in the list and add it in the geofencingclient.
        for (FavoriteProviderEntity entity : favoriteList) {
            log.i("Favorite: %s, %s, %s", entity.providerName, entity.category, entity.providerId);
            geofenceList.add(new Geofence.Builder()
                    .setRequestId(entity.providerId)
                    .setCircularRegion(entity.latitude, entity.longitude, Constants.GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setLoiteringDelay(Constants.GEOFENCE_LOITERING_TIME)
                    .build());
        }

        // Check permission. In this case that registered geofences should be reset, the permission
        // should have already been given b/c it occurs when the device is rebooted or reinstalled.
        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            // public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnSuccessListener(Void -> log.i("Add geofences successfully"))
                .addOnFailureListener(e -> log.e("Adding geofences failed"));

    }



    // GeofencingRequest and its nested GeofencingRequestBuilder is to specify the geofences to monitor
    // and to set how related geofence events are triggered.
    // Initial trigger should be preferred to set INITIAL_TRIGGER_DWELL for reducing alert spam.
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder requestBuilder = new GeofencingRequest.Builder();
        requestBuilder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        requestBuilder.addGeofences(geofenceList);
        return requestBuilder.build();
    }


    private PendingIntent getGeofencePendingIntent() {
        if(mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }

        // To get the same pending intent back when calling addGeofences() and removeGeofences(),
        // use FLAG_UPDATE_CURRENT.
        //Intent geoIntent = new Intent(context, GeofenceTransitionService.class);
        Intent geoIntent = new Intent(context, GeofenceBroadcastReceiver.class);
        geoIntent.setAction(Constants.NOTI_GEOFENCE);
        mGeofencePendingIntent = PendingIntent.getBroadcast(context, 0, geoIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return mGeofencePendingIntent;
    }
}
