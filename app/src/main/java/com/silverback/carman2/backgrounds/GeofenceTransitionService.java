package com.silverback.carman2.backgrounds;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.FavoriteProviderEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class GeofenceTransitionService extends IntentService {

    private static final LoggingHelper log = LoggingHelperFactory.create(GeofenceTransitionService.class);

    // Objects
    private CarmanDatabase mDB;
    private Location geofenceLocation;
    private String providerId;


    public GeofenceTransitionService() {
        super("GeofenceTransitionService");

    }

    @Override
    protected void onHandleIntent(Intent intent) {

        mDB = CarmanDatabase.getDatabaseInstance(getApplicationContext());
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        providerId = intent.getStringExtra("providerId");

        if(geofencingEvent != null) {
            log.i("GeofencingEvent");
            if(geofencingEvent.hasError()) {
                log.e("GeofencingEvent error occurred: %s", geofencingEvent.getErrorCode());
                return;
            }

            int geofencingTransition = geofencingEvent.getGeofenceTransition();
            switch(geofencingTransition) {
                case Geofence.GEOFENCE_TRANSITION_ENTER:
                    break;

                case Geofence.GEOFENCE_TRANSITION_EXIT:
                    break;

                case Geofence.GEOFENCE_TRANSITION_DWELL:
                    break;

                default:
            }


            geofenceLocation = geofencingEvent.getTriggeringLocation();

            FavoriteProviderEntity entity = mDB.favoriteModel().findFavoriteProvider(null, providerId);
            log.i("Entity: %s", entity);

        }

    }


}
