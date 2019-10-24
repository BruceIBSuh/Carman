package com.silverback.carman2.backgrounds;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.List;

public class GeofenceReceiver extends BroadcastReceiver {

    private static final LoggingHelper log = LoggingHelperFactory.create(GeofenceReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {

        // an Intent broadcast.
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            //String errorMessage = GeofenceErrorMessages.getErrorString(this, geofencingEvent.getErrorCode());
            log.e("GeofenceErrorMessage: %s");
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            for(Geofence geofence : triggeringGeofences) {
                log.i("Geofence Transition: %s", geofence.getRequestId());
            }

            // Get the transition details as a String.
            /*
            String geofenceTransitionDetails = getGeofenceTransitionDetails(
                    this,
                    geofenceTransition,
                    triggeringGeofences
            );
            */

            // Send notification and log the transition details.
            //sendNotification(geofenceTransitionDetails);
            //log.i("GeofenceTransitionDetails: %s", geofenceTransitionDetails);
        } else {
            // Log the error.
            log.e("GeorenceTransition failded");
        }
    }
}
