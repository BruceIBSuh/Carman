package com.silverback.carman2.backgrounds;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
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
        log.i("Favorite: %s", favoriteList.size());

        for(FavoriteProviderEntity entity : favoriteList) {
            log.i("Favorite: %s, %s, %s", entity.providerName, entity.category, entity.providerId);
            geofenceList.add(new Geofence.Builder()
                    .setRequestId(entity.providerId)
                    .setCircularRegion(entity.latitude, entity.longitude, Constants.GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    //.setLoiteringDelay(Constants.GEOFENCE_LOITERING_TIME)
                    .build());
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
